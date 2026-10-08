#!/usr/bin/env python3
"""Execute the real upstream emulator's --help without distributing firmware."""
import argparse
import pathlib
import subprocess
import sys
import zipfile


def verify(jar, timeout=35):
    jar = pathlib.Path(jar)
    if not jar.is_file():
        raise ValueError('JAR not found')
    with zipfile.ZipFile(jar) as archive:
        manifest = archive.read('META-INF/MANIFEST.MF').decode('utf-8', 'replace')
        if 'Main-Class:' not in manifest:
            raise ValueError('JAR is not executable (missing Main-Class)')
    proc = subprocess.run(['java', '-jar', str(jar), '--help'], capture_output=True,
                          text=True, timeout=timeout, check=False)
    output = (proc.stdout + '\n' + proc.stderr).strip()
    if proc.returncode != 0:
        raise RuntimeError(f'Upstream --help returned {proc.returncode}: {output[:800]}')
    if not any(token in output.lower() for token in ('cybiko', 'machine', 'nvram', 'usage')):
        raise RuntimeError(f'Unrecognized emulator help output: {output[:800]}')
    return output


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('jar')
    args = parser.parse_args()
    try:
        result = verify(args.jar)
    except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired, zipfile.BadZipFile, KeyError) as error:
        print('FAIL:', error, file=sys.stderr)
        return 1
    print('PASS: Real upstream executable JAR responded to --help')
    print(result[:1200])
    return 0

if __name__ == '__main__':
    sys.exit(main())
