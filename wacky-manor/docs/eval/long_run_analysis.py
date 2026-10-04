import json

base = "/Users/mdproctor/claude/casehub/slots/196/examples/wacky-manor/docs/eval"

def analyze(path, label):
    results = json.load(open(path))
    char_scores = {}
    for r in results:
        cid = r["character"]
        for drive, val in r["scores"].items():
            if isinstance(val, dict) and "score" in val:
                char_scores.setdefault(cid, {}).setdefault(drive, []).append(val["score"])
    return char_scores

short = analyze(f"{base}/layer1-thinking-capture-generic-20261004/classification.json", "L1 short")
long = analyze(f"{base}/layer1-long-run-generic-20261004/classification.json", "L1 long")

chars = ["hooded-claw", "peter-perfect", "penelope-pitstop", "dick-dastardly", "ant-hill-mob"]

print("=== SHORT (222 events) vs LONG (516 events) — Average drive expression ===\n")
for cid in chars:
    print(f"{cid}:")
    for drive in sorted(long.get(cid, {})):
        s_avg = sum(short[cid][drive]) / len(short[cid][drive]) if cid in short and drive in short.get(cid, {}) else 0
        l_avg = sum(long[cid][drive]) / len(long[cid][drive])
        delta = l_avg - s_avg
        print(f"  {drive:20s}  short={s_avg:.2f} (n={len(short.get(cid,{}).get(drive,[]))})  long={l_avg:.2f} (n={len(long[cid][drive])})  delta={delta:+.2f}")
    print()

print("\n=== LONG RUN — QUARTER-BY-QUARTER TEMPORAL ANALYSIS ===\n")
for cid in chars:
    scores = long.get(cid, {})
    if not scores:
        continue
    print(f"{cid}:")
    for drive in sorted(scores):
        s = scores[drive]
        n = len(s)
        q = n // 4
        if q == 0:
            continue
        q1 = sum(s[:q]) / q
        q2 = sum(s[q:2*q]) / q
        q3 = sum(s[2*q:3*q]) / q
        q4 = sum(s[3*q:]) / (n - 3*q)
        trend = q4 - q1
        print(f"  {drive:20s}  Q1={q1:.2f}  Q2={q2:.2f}  Q3={q3:.2f}  Q4={q4:.2f}  trend(Q4-Q1)={trend:+.2f}")
    print()

print("\n=== OVERALL MEAN DRIVE EXPRESSION ===")
for label, data in [("L1 short (222)", short), ("L1 long (516)", long)]:
    all_avgs = []
    for cid in data:
        for drive in data[cid]:
            all_avgs.append(sum(data[cid][drive]) / len(data[cid][drive]))
    print(f"  {label:25s}  mean={sum(all_avgs)/len(all_avgs):.2f}")

print("\n=== TEMPORAL STABILITY (mean |Q4-Q1| across all drives) ===")
all_trends = []
for cid in chars:
    scores = long.get(cid, {})
    for drive in scores:
        s = scores[drive]
        n = len(s)
        q = n // 4
        if q == 0:
            continue
        q1 = sum(s[:q]) / q
        q4 = sum(s[3*q:]) / (n - 3*q)
        all_trends.append(abs(q4 - q1))
print(f"  Long run mean |Q4-Q1|={sum(all_trends)/len(all_trends):.2f}")

# Compare with short run halves
all_short_trends = []
for cid in chars:
    scores = short.get(cid, {})
    if not scores:
        continue
    for drive in scores:
        s = scores[drive]
        n = len(s)
        half = n // 2
        if half == 0:
            continue
        h1 = sum(s[:half]) / half
        h2 = sum(s[half:]) / (n - half)
        all_short_trends.append(abs(h2 - h1))
print(f"  Short run mean |H2-H1|={sum(all_short_trends)/len(all_short_trends):.2f}")
