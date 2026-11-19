---
id: PP-20261119-8971fd
title: "Append issues to the primary workspace queue only"
type: rule
scope: platform
applies_to: "any multi-workspace slot — filing or scheduling issues"
severity: critical
violation_hint: "Items added to wsp-casehub-neocortex/.queue or other per-repo workspace queues are invisible to work.py"
created: 2026-11-19
---

In a multi-workspace slot, always append new issues to the primary workspace
queue (`wsp-casehub/.queue`). The work pipeline (`work.py` via `ctx.py`)
resolves to this queue exclusively. Per-repo workspace queues
(`wsp-casehub-neocortex/.queue`, `wsp-casehub-eidos/.queue`, etc.) are
historical artifacts from earlier tracking eras and are not read by the
pipeline. Items added there are silently lost to `work continue` / `work next`.
