import json, re

planning_kw = ["plan", "phase", "step", "prepare", "schedule", "systematically", "protocol", "procedure", "stage"]
optimism_kw = ["determination", "undaunted", "pressing on", "shall not be deterred", "brave", "gallant", "persevere", "onward", "courage", "bold"]
thirdperson_kw = ["hartwell", "james hartwell"]
scheme_kw = ["step one", "step two", "step three", "firstly", "secondly", "thirdly", "phase one", "phase two", "part one", "part two", "my plan", "the plan is", "step 1", "step 2", "step 3", "step four", "step five"]

def load(path):
    with open(path, "r") as f:
        raw = f.read()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        fixed = re.sub(r'(?<=": ")([^"]*?)(?=")', lambda m: m.group().replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t'), raw)
        return json.loads(fixed)

def count_matches(events, char_id, keywords, types=("dialogue", "aside")):
    filtered = [e for e in events if e.get("character") == char_id and e.get("type") in types]
    total = len(filtered)
    hits = 0
    for e in filtered:
        desc = (e.get("desc") or "").lower()
        if any(kw in desc for kw in keywords):
            hits += 1
    return hits, total

def fmt(hits, total):
    pct = hits/total*100 if total else 0
    return f"{hits}/{total} ({pct:.1f}%)"

base = "/Users/mdproctor/claude/casehub/slots/196/examples/wacky-manor/docs/eval"
files = {
    "Baseline": f"{base}/emotional-core-generic-full-20261003/transcript.json",
    "R3": f"{base}/pareback-r3-generic-20261003/transcript.json",
    "R3b": f"{base}/pareback-r3b-generic-20261003/transcript.json",
}

print("=== HARTWELL (peter-perfect) ===")
for label, kw in [("Planning", planning_kw), ("Optimism", optimism_kw), ("Third-person", thirdperson_kw)]:
    results = []
    for name, path in files.items():
        d = load(path)
        hits, total = count_matches(d["events"], "peter-perfect", kw)
        results.append(fmt(hits, total))
    print(f"  {label}: Baseline={results[0]}  R3={results[1]}  R3b={results[2]}")

print()
print("=== VILLAINS (scheme narration in asides) ===")
for char_id, char_name in [("hooded-claw", "Marsh"), ("dick-dastardly", "Foxworth")]:
    results = []
    for name, path in files.items():
        d = load(path)
        hits, total = count_matches(d["events"], char_id, scheme_kw, types=("aside",))
        results.append(fmt(hits, total))
    print(f"  {char_name}: Baseline={results[0]}  R3={results[1]}  R3b={results[2]}")
