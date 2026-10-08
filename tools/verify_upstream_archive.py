#!/usr/bin/env python3
"""Safely inspect the source archive produced by upstream-source-audit.yml."""
import argparse
import hashlib
import pathlib
import tarfile


def inspect(path):
    with tarfile.open(path, 'r:gz') as archive:
        members = archive.getmembers()
        for member in members:
            p = pathlib.PurePosixPath(member.name)
            if p.is_absolute() or '..' in p.parts or member.issym() or member.islnk() or member.isdev():
                raise ValueError(f'Unsafe archive member: {member.name}')
        names = {m.name.lstrip('./') for m in members}
        required = {'upstream/LICENSE', 'upstream/README.md', 'upstream-revision.txt'}
        missing = required - names
        if missing:
            raise ValueError(f'Missing files: {sorted(missing)}')
        sources = [n for n in names if n.startswith('upstream/emulator/') and n.endswith('.java')]
        if not sources:
            raise ValueError('No Java emulator sources')
        license_text = archive.extractfile(next(m for m in members if m.name.lstrip('./') == 'upstream/LICENSE')).read()
        if b'MIT License' not in license_text:
            raise ValueError('Unexpected license')
        print(f'Archive valid: {len(sources)} emulator Java sources')
        print(f'MIT license SHA-256: {hashlib.sha256(license_text).hexdigest()}')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('archive', type=pathlib.Path)
    inspect(parser.parse_args().archive)
