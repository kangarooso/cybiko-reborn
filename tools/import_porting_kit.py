#!/usr/bin/env python3
"""Safely stage the source artifact from upstream-compatibility.yml.

This is a reference import, not an Android emulator integration.
"""
import io, pathlib, tarfile, sys, shutil
ROOT = pathlib.Path(__file__).resolve().parents[1]
DEST = ROOT / 'upstream-reference'

def stage(archive, dest=DEST):
    archive = pathlib.Path(archive)
    if dest.exists(): raise ValueError('Destination exists; refusing to overwrite')
    with tarfile.open(archive, 'r:gz') as tf:
        members = tf.getmembers()
        if len(members) > 20000: raise ValueError('Too many archive members')
        files = {}
        for m in members:
            path = pathlib.PurePosixPath(m.name)
            if path.is_absolute() or '..' in path.parts or not (m.isfile() or m.isdir()):
                raise ValueError('Unsafe archive entry: ' + m.name)
            if m.size > 30_000_000: raise ValueError('Oversized entry')
            if m.isfile(): files[str(path).removeprefix('./')] = m
        license_file = next((k for k in ('UPSTREAM_LICENSE.txt','LICENSE') if k in files), None)
        if not license_file: raise ValueError('License not included')
        if not any(k.startswith('emulator/') and k.endswith('.java') for k in files):
            raise ValueError('Missing Java emulator source')
        lic = tf.extractfile(files[license_file]).read()
        if b'MIT License' not in lic: raise ValueError('Expected MIT license missing')
        dest.mkdir(parents=True)
        try:
            for name, member in files.items():
                if not (name.startswith('emulator/') or name in ('UPSTREAM_LICENSE.txt','UPSTREAM_COMMIT.txt','ANDROID_PORT_REPORT.md','JAVA_SOURCES.txt','LICENSE')):
                    continue
                output = dest / name
                output.parent.mkdir(parents=True,exist_ok=True)
                with tf.extractfile(member) as inp, output.open('wb') as out:
                    shutil.copyfileobj(inp,out)
        except Exception:
            shutil.rmtree(dest)
            raise
    return dest

if __name__ == '__main__':
    if len(sys.argv) != 2: raise SystemExit('Usage: python3 tools/import_porting_kit.py upstream-porting-kit.tar.gz')
    print('Staged:',stage(sys.argv[1]))
    print('Original CPU core still needs Android porting; no CyOS boot claimed.')
