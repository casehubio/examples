#!/usr/bin/env python3
"""
Sync example subtrees from source repos.

Reads sync-config.json for the repo list. In CI mode, pins each clone
to the SHA from the ecosystem-build-succeeded dispatch payload. In
manual/local mode, uses HEAD.
"""

import json, os, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
CONFIG = json.loads((ROOT / 'sync-config.json').read_text())

raw_shas = os.environ.get('SHAS_JSON', '{}')
shas = json.loads(raw_shas) if raw_shas and raw_shas != 'null' else {}
trigger = os.environ.get('TRIGGER', 'manual')
token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN', '')

synced = []
failed = []

for entry in CONFIG['repos']:
    name = entry['name']
    org = entry['org']
    repo = entry.get('repo', name)
    prefix = f"{name}-examples"

    # Resolve SHA: underscore key convention (blocks-ui -> blocks_ui)
    sha_key = name.replace('-', '_')
    sha = shas.get(sha_key, '')

    print(f"\n--- {name} ---")

    with tempfile.TemporaryDirectory() as tmpdir:
        clone_dir = os.path.join(tmpdir, name)
        url = f"https://x-access-token:{token}@github.com/{org}/{repo}.git"

        try:
            subprocess.run(
                ['git', 'clone', '--quiet', url, clone_dir],
                check=True, capture_output=True
            )
        except subprocess.CalledProcessError:
            print(f"  SKIP: could not clone {org}/{repo}")
            failed.append(name)
            continue

        if sha:
            try:
                subprocess.run(
                    ['git', '-C', clone_dir, 'checkout', sha, '--quiet'],
                    check=True, capture_output=True
                )
                print(f"  Pinned to {sha[:12]}")
            except subprocess.CalledProcessError:
                print(f"  WARN: SHA {sha[:12]} not found, using HEAD")

        if not os.path.isdir(os.path.join(clone_dir, 'examples')):
            print(f"  SKIP: no examples/ directory")
            continue

        try:
            subprocess.run(
                ['git', '-C', clone_dir, 'subtree', 'split',
                 '--prefix=examples', '-b', 'examples-only', '--quiet'],
                check=True, capture_output=True
            )
        except subprocess.CalledProcessError as e:
            print(f"  SKIP: subtree split failed — {e}")
            failed.append(name)
            continue

        prefix_dir = ROOT / prefix
        sha_label = sha[:12] if sha else 'HEAD'
        msg = f"sync: {name} examples ({trigger}, {sha_label})"

        try:
            if prefix_dir.exists():
                subprocess.run(
                    ['git', '-C', str(ROOT), 'subtree', 'pull',
                     f'--prefix={prefix}', clone_dir, 'examples-only',
                     '--squash', '-m', msg],
                    check=True, capture_output=True
                )
                print(f"  Updated {prefix}")
            else:
                subprocess.run(
                    ['git', '-C', str(ROOT), 'subtree', 'add',
                     f'--prefix={prefix}', clone_dir, 'examples-only',
                     '--squash', '-m', msg],
                    check=True, capture_output=True
                )
                print(f"  Seeded {prefix}")
            synced.append(name)
        except subprocess.CalledProcessError as e:
            print(f"  WARN: subtree {'pull' if prefix_dir.exists() else 'add'} "
                  f"failed — {e}")
            failed.append(name)

print(f"\n=== Summary ===")
print(f"Synced: {len(synced)} ({', '.join(synced) if synced else 'none'})")
if failed:
    print(f"Failed: {len(failed)} ({', '.join(failed)})")
