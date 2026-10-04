import json

def analyze(path):
    results = json.load(open(path))
    char_scores = {}
    for r in results:
        cid = r["character"]
        for drive, val in r["scores"].items():
            if isinstance(val, dict) and "score" in val:
                char_scores.setdefault(cid, {}).setdefault(drive, []).append(val["score"])

    out = {}
    for cid in sorted(char_scores):
        out[cid] = {}
        for drive in sorted(char_scores[cid]):
            scores = char_scores[cid][drive]
            n = len(scores)
            half = n // 2
            avg = sum(scores) / n
            avg1 = sum(scores[:half]) / half if half > 0 else 0
            avg2 = sum(scores[half:]) / (n - half) if (n - half) > 0 else 0
            out[cid][drive] = {"avg": round(avg, 2), "n": n, "h1": round(avg1, 2), "h2": round(avg2, 2), "delta": round(avg2 - avg1, 2)}
    return out

base = "/Users/mdproctor/claude/casehub/slots/196/examples/wacky-manor/docs/eval"
labels = ["R3 (none)", "R3b (passive)", "R3g (evocative)", "L1 (echo)"]
paths = [
    base + "/pareback-r3-generic-20261003/classification.json",
    base + "/pareback-r3b-generic-20261003/classification.json",
    base + "/pareback-r3g-full-generic-20261004/classification.json",
    base + "/layer1-thinking-capture-generic-20261004/classification.json",
]
runs = [analyze(p) for p in paths]

chars = ["hooded-claw", "peter-perfect", "penelope-pitstop", "dick-dastardly", "ant-hill-mob"]
for cid in chars:
    print(f"\n=== {cid} ===")
    all_drives = set()
    for run in runs:
        if cid in run:
            all_drives.update(run[cid].keys())
    for drive in sorted(all_drives):
        print(f"  {drive}:")
        for i, run in enumerate(runs):
            label = labels[i]
            d = run.get(cid, {}).get(drive, {})
            if d:
                print(f"    {label:20s}  avg={d['avg']:.2f}  1st={d['h1']:.2f}  2nd={d['h2']:.2f}  delta={d['delta']:+.2f}  (n={d['n']})")
            else:
                print(f"    {label:20s}  --")

print("\n\n=== OVERALL MEAN DRIVE EXPRESSION (all characters, all drives) ===")
for i, run in enumerate(runs):
    all_scores = []
    for cid in run:
        for drive in run[cid]:
            all_scores.append(run[cid][drive]["avg"])
    mean = sum(all_scores) / len(all_scores) if all_scores else 0
    print(f"  {labels[i]:20s}  mean={mean:.2f}  (across {len(all_scores)} character-drive pairs)")

print("\n=== PER-CHARACTER MEAN DRIVE EXPRESSION ===")
for cid in chars:
    print(f"  {cid}:")
    for i, run in enumerate(runs):
        if cid in run:
            avgs = [run[cid][d]["avg"] for d in run[cid]]
            mean = sum(avgs) / len(avgs)
            print(f"    {labels[i]:20s}  mean={mean:.2f}")

print("\n=== TEMPORAL STABILITY (mean |delta| across all drives) ===")
for i, run in enumerate(runs):
    all_deltas = []
    for cid in run:
        for drive in run[cid]:
            all_deltas.append(abs(run[cid][drive]["delta"]))
    mean_delta = sum(all_deltas) / len(all_deltas) if all_deltas else 0
    print(f"  {labels[i]:20s}  mean |delta|={mean_delta:.2f}")
