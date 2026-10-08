#!/usr/bin/env python3
"""Static inventory of desktop-specific dependencies in upstream Java sources."""
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
if not root.is_dir():
    raise SystemExit(f"Missing source directory: {root}")
files = sorted(root.rglob('*.java'))
if not files:
    raise SystemExit('No Java source files found')
patterns = {
    'Swing/AWT UI': r'\b(?:javax\.swing|java\.awt)\b',
    'JavaFX UI': r'\bjavafx\.',
    'Desktop sound': r'\bjavax\.sound\.',
    'Desktop file dialogs': r'\b(?:JFileChooser|FileDialog)\b',
    'Network/SDR': r'\b(?:MulticastSocket|DatagramSocket|Socket)\b',
}
print('# Android compatibility inventory\n')
print(f'Java source files scanned: **{len(files)}**\n')
print('Matches are static indicators, not proof of incompatibility.\n')
for category, pattern in patterns.items():
    matches = []
    for file in files:
        if re.search(pattern, file.read_text(errors='replace')):
            matches.append(file.relative_to(root).as_posix())
    print(f'## {category} ({len(matches)} files)\n')
    for name in matches:
        print(f'- `{name}`')
    print()
