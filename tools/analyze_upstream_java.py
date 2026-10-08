#!/usr/bin/env python3
"""Produce reproducible Android port inventory from real upstream Java sources."""
import argparse, json, pathlib, re, sys

def analyze(root):
    root = pathlib.Path(root)
    files = sorted(root.rglob('*.java'))
    if not files: raise ValueError('No upstream Java source files found')
    report = {'source_files':len(files), 'desktop_ui':[], 'android_incompatible':[], 'portable_candidates':[]}
    ui = re.compile(r'\b(?:javax\.swing|java\.awt|javafx)\b')
    incompatible = re.compile(r'\b(?:java\.lang\.foreign|jdk\.internal|sun\.misc)\b')
    for f in files:
        code=f.read_text(encoding='utf-8',errors='replace')
        rel=f.relative_to(root).as_posix()
        if ui.search(code): report['desktop_ui'].append(rel)
        elif incompatible.search(code): report['android_incompatible'].append(rel)
        else: report['portable_candidates'].append(rel)
    return report

def main():
    p=argparse.ArgumentParser(); p.add_argument('source_root'); p.add_argument('--output',default='upstream-port-inventory.json')
    args=p.parse_args()
    try: report=analyze(args.source_root)
    except (ValueError,OSError) as e: print('ERROR:',e,file=sys.stderr); return 1
    pathlib.Path(args.output).write_text(json.dumps(report,indent=2)+'\n')
    print('Files:',report['source_files'],'desktop UI:',len(report['desktop_ui']), 'incompatible:',len(report['android_incompatible']), 'portable candidates:',len(report['portable_candidates']))
    return 0
if __name__=='__main__': sys.exit(main())
