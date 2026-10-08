#!/usr/bin/env python3
"""Run the REAL upstream Cybiko Classic V1 emulator headlessly.

This does not emulate anything itself. A successful exit from a timeout is NOT
proof of a successful CyOS boot; the log must be inspected for CyOS activity.
"""
import argparse
import pathlib
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=pathlib.Path, required=True)
    parser.add_argument('--boot', type=pathlib.Path, required=True)
    parser.add_argument('--flash', type=pathlib.Path, required=True)
    parser.add_argument('--seconds', type=int, default=30)
    parser.add_argument('--log', type=pathlib.Path, default=pathlib.Path('cybiko-boot.log'))
    args = parser.parse_args()
    for name in ('jar', 'boot', 'flash'):
        f = getattr(args, name)
        if not f.is_file() or f.stat().st_size == 0:
            parser.error(f'{name} must be an existing nonempty file: {f}')
    if not 1 <= args.seconds <= 300:
        parser.error('--seconds must be between 1 and 300')
    cmd = ['java', '-jar', str(args.jar.resolve()), '--machine', 'v1',
           str(args.boot.resolve()), str(args.flash.resolve()),
           '--headless', '--mute', '--logging', 'boot,status']
    try:
        result = subprocess.run(cmd, capture_output=True, text=True,
                                timeout=args.seconds, check=False)
        output = result.stdout + '\n' + result.stderr
        args.log.write_text(output)
        print(f'Emulator exited with status {result.returncode}; log: {args.log}')
        if result.returncode != 0:
            print('FAIL: emulator process failed; inspect log')
            return 1
        print('INCONCLUSIVE: process exited; inspect log for CyOS boot evidence')
        return 2
    except subprocess.TimeoutExpired as exc:
        output = (exc.stdout or b'').decode(errors='replace') if isinstance(exc.stdout, bytes) else (exc.stdout or '')
        err = (exc.stderr or b'').decode(errors='replace') if isinstance(exc.stderr, bytes) else (exc.stderr or '')
        args.log.write_text(output + '\n' + err)
        print(f'INCONCLUSIVE: emulator ran for {args.seconds}s; inspect {args.log} for boot evidence')
        return 2

if __name__ == '__main__':
    sys.exit(main())
