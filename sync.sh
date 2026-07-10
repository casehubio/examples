#!/bin/bash
set -euo pipefail

# Update all example subtrees to HEAD of each source repo.
# Analogous to casehub-all's sync.sh, adapted for subtrees.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG="$SCRIPT_DIR/sync-config.json"
TMPDIR=$(mktemp -d)

trap 'rm -rf "$TMPDIR"' EXIT

echo "Syncing examples to HEAD..."

python3 -c "
import json, sys
config = json.load(open('$CONFIG'))
for r in config['repos']:
    repo = r.get('repo', r['name'])
    print(f\"{r['name']} {r['org']} {repo}\")
" | while read -r name org repo; do
  repo_url="https://github.com/$org/$repo.git"
  clone_dir="$TMPDIR/$name"
  prefix="${name}-examples"

  echo ""
  echo "--- $name ---"

  git clone --quiet "$repo_url" "$clone_dir" 2>/dev/null || {
    echo "  SKIP: could not clone $repo_url"
    continue
  }

  if [ ! -d "$clone_dir/examples" ]; then
    echo "  SKIP: no examples/ directory"
    rm -rf "$clone_dir"
    continue
  fi

  (cd "$clone_dir" && git subtree split --prefix=examples -b examples-only --quiet) || {
    echo "  SKIP: subtree split failed"
    rm -rf "$clone_dir"
    continue
  }

  if [ -d "$SCRIPT_DIR/$prefix" ]; then
    git subtree pull --prefix="$prefix" "$clone_dir" examples-only --squash \
      -m "sync: $name examples to HEAD" --quiet 2>/dev/null || {
      echo "  WARN: subtree pull failed (may need manual merge)"
      rm -rf "$clone_dir"
      continue
    }
    echo "  Updated $prefix"
  else
    git subtree add --prefix="$prefix" "$clone_dir" examples-only --squash \
      -m "sync: seed $name examples from HEAD" --quiet 2>/dev/null || {
      echo "  WARN: subtree add failed"
      rm -rf "$clone_dir"
      continue
    }
    echo "  Seeded $prefix"
  fi

  rm -rf "$clone_dir"
done

echo ""
echo "Sync complete."
