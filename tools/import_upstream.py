#!/usr/bin/env python3
"""Import a locally available upstream source archive without bundling firmware."""
import pathlib, sys, zipfile, shutil
root=pathlib.Path(__file__).resolve().parents[1]
if len(sys.argv)!=2:
    raise SystemExit('Usage: python3 tools/import_upstream.py /path/to/cybiko-java-emulator.zip')
archive=pathlib.Path(sys.argv[1])
if not zipfile.is_zipfile(archive): raise SystemExit('Not a ZIP archive')
dest=root/'upstream-reference'
if dest.exists(): raise SystemExit('Destination already exists; remove it before importing')
with zipfile.ZipFile(archive) as z:
    names=z.namelist()
    if not any('/emulator/src/' in n for n in names): raise SystemExit('Missing upstream emulator/src tree')
    if not any(n.endswith('/LICENSE') or n=='LICENSE' for n in names): raise SystemExit('Missing upstream license')
    for item in z.infolist():
        p=pathlib.PurePosixPath(item.filename)
        if p.is_absolute() or '..' in p.parts: raise SystemExit('Unsafe ZIP path')
        if item.file_size>30_000_000: raise SystemExit('Unexpected large file')
    dest.mkdir()
    for item in z.infolist():
        p=pathlib.PurePosixPath(item.filename)
        if len(p.parts)<2 or item.is_dir(): continue
        rel=pathlib.Path(*p.parts[1:])
        if rel.parts[0] not in ('emulator','LICENSE','README.md','build.gradle','settings.gradle'):continue
        out=dest/rel;out.parent.mkdir(parents=True,exist_ok=True)
        with z.open(item) as src,open(out,'wb') as dst: shutil.copyfileobj(src,dst)
print('Imported upstream reference sources; Android port and API adaptation still required.')
