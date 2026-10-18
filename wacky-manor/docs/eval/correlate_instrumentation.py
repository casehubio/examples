#!/usr/bin/env python3
"""Correlate pipeline instrumentation logs with classifier scores.

Parses [appraisal-filter], [differentiation-filter], and [observation-assembly]
lines from the server log, aligns them with per-event classifier scores, and
tests the differentiation text dilution hypothesis.

Usage:
    python3 correlate_instrumentation.py <server_log> <classifier_results_json>

    # Or just parse instrumentation logs without classifier:
    python3 correlate_instrumentation.py <server_log> --log-only
"""

import argparse
import json
import re
import sys
from collections import defaultdict


def parse_appraisal_filter(line):
    m = re.match(
        r'\[appraisal-filter\]\s+(\S+)\s+\|\s+situation=\[([^\]]*)\]\s+\|\s+ratios=\[([^\]]*)\]\s+\|\s+rendered=\[([^\]]*)\]\s+\|\s+filtered=\[([^\]]*)\]',
        line
    )
    if not m:
        return None
    agent_id = m.group(1)
    situation_raw = m.group(2).strip()
    ratios_raw = m.group(3).strip()
    rendered = [x.strip() for x in m.group(4).split(',') if x.strip()]
    filtered = [x.strip() for x in m.group(5).split(',') if x.strip()]

    situations = []
    if situation_raw:
        for part in situation_raw.split(','):
            part = part.strip()
            if ':' in part:
                sid, conf = part.rsplit(':', 1)
                situations.append({'id': sid.strip(), 'confidence': float(conf)})

    ratios = {}
    if ratios_raw:
        for part in ratios_raw.split('),'):
            part = part.strip()
            if '=' not in part:
                continue
            drive, rest = part.split('=', 1)
            ratios[drive.strip()] = rest.strip().rstrip(')')

    return {
        'agent_id': agent_id,
        'situations': situations,
        'ratios': ratios,
        'rendered': rendered,
        'filtered': filtered,
    }


def parse_differentiation_filter(line):
    m = re.match(
        r'\[differentiation-filter\]\s+(\S+)\s+\|\s+total=(\d+)\s+\|\s+included=(\d+)\s+\|\s+excluded=(\d+)\s+\[([^\]]*)\]\s+\|\s+activeDrives=\[([^\]]*)\]',
        line
    )
    if not m:
        return None
    return {
        'agent_id': m.group(1),
        'total': int(m.group(2)),
        'included': int(m.group(3)),
        'excluded': int(m.group(4)),
        'excluded_pairs': [x.strip() for x in m.group(5).split(',') if x.strip()],
        'active_drives': [x.strip() for x in m.group(6).split(',') if x.strip()],
    }


def parse_observation_assembly(line):
    m = re.match(
        r'\[observation-assembly\]\s+(\S+)\s+\|\s+drives=(\d+)\s+\[([^\]]*)\]\s+\|\s+diffTexts=(\d+)\s+\|\s+memories=(\d+)\s+\|\s+attractors=(\d+)\s+\|\s+totalParts=(\d+)',
        line
    )
    if not m:
        return None
    return {
        'agent_id': m.group(1),
        'drives': int(m.group(2)),
        'drive_names': [x.strip() for x in m.group(3).split(',') if x.strip()],
        'diff_texts': int(m.group(4)),
        'memories': int(m.group(5)),
        'attractors': int(m.group(6)),
        'total_parts': int(m.group(7)),
    }


def parse_server_log(log_path):
    """Parse instrumentation lines from server log, grouping into per-invocation records."""
    records_by_agent = defaultdict(list)
    current = {}

    with open(log_path) as f:
        for line in f:
            line = line.strip()

            if '[appraisal-filter]' in line:
                idx = line.index('[appraisal-filter]')
                parsed = parse_appraisal_filter(line[idx:])
                if parsed:
                    agent = parsed['agent_id']
                    if agent in current and 'appraisal' in current[agent]:
                        records_by_agent[agent].append(current.pop(agent))
                    current.setdefault(agent, {})['appraisal'] = parsed

            elif '[differentiation-filter]' in line:
                idx = line.index('[differentiation-filter]')
                parsed = parse_differentiation_filter(line[idx:])
                if parsed:
                    agent = parsed['agent_id']
                    current.setdefault(agent, {})['differentiation'] = parsed

            elif '[observation-assembly]' in line:
                idx = line.index('[observation-assembly]')
                parsed = parse_observation_assembly(line[idx:])
                if parsed:
                    agent = parsed['agent_id']
                    current.setdefault(agent, {})['assembly'] = parsed
                    records_by_agent[agent].append(current.pop(agent))

    for agent, rec in current.items():
        records_by_agent[agent].append(rec)

    return dict(records_by_agent)


def print_instrumentation_summary(records_by_agent):
    print("\n=== INSTRUMENTATION SUMMARY ===\n")

    for agent in sorted(records_by_agent):
        records = records_by_agent[agent]
        print(f"\n--- {agent} ({len(records)} events) ---")

        classifier_bypassed = 0
        classifier_activated = 0
        diff_text_counts = []
        total_parts_counts = []
        rendered_drive_sets = []
        filtered_drive_sets = []

        for rec in records:
            asm = rec.get('assembly', {})
            appr = rec.get('appraisal')
            diff = rec.get('differentiation')

            if appr:
                classifier_activated += 1
                rendered_drive_sets.append(set(appr.get('rendered', [])))
                filtered_drive_sets.append(set(appr.get('filtered', [])))
            else:
                classifier_bypassed += 1

            if asm:
                diff_text_counts.append(asm.get('diff_texts', 0))
                total_parts_counts.append(asm.get('total_parts', 0))

        print(f"  Classifier: activated={classifier_activated}, bypassed={classifier_bypassed}")
        if diff_text_counts:
            avg_diff = sum(diff_text_counts) / len(diff_text_counts)
            max_diff = max(diff_text_counts)
            min_diff = min(diff_text_counts)
            print(f"  DiffTexts: avg={avg_diff:.1f}, min={min_diff}, max={max_diff}")
            dist = defaultdict(int)
            for c in diff_text_counts:
                dist[c] += 1
            print(f"  DiffTexts distribution: {dict(sorted(dist.items()))}")
        if total_parts_counts:
            avg_parts = sum(total_parts_counts) / len(total_parts_counts)
            print(f"  TotalParts: avg={avg_parts:.1f}")

        if filtered_drive_sets:
            all_filtered = defaultdict(int)
            for fs in filtered_drive_sets:
                for d in fs:
                    all_filtered[d] += 1
            print(f"  Filtered drives: {dict(all_filtered)}")


def correlate_with_scores(records_by_agent, classifier_results):
    """Correlate per-event instrumentation data with classifier scores."""
    print("\n=== DILUTION HYPOTHESIS TEST ===\n")

    events_by_agent = defaultdict(list)
    for result in classifier_results:
        agent = result.get('character', 'unknown')
        events_by_agent[agent].append(result)

    for agent in sorted(records_by_agent):
        records = records_by_agent[agent]
        events = events_by_agent.get(agent, [])

        if not events:
            print(f"{agent}: no classifier scores available")
            continue

        if len(records) != len(events):
            print(f"{agent}: WARNING — {len(records)} instrumentation records vs {len(events)} classifier events (alignment may be off)")

        n = min(len(records), len(events))

        protection_scores_by_diff_count = defaultdict(list)
        all_drive_scores_by_diff_count = defaultdict(lambda: defaultdict(list))

        for i in range(n):
            rec = records[i]
            evt = events[i]
            asm = rec.get('assembly', {})
            diff_count = asm.get('diff_texts', -1)
            scores = evt.get('scores', {})

            for drive, val in scores.items():
                if not isinstance(val, dict):
                    continue
                score = val.get('score')
                relevant = val.get('relevant', True)
                if score is not None and relevant:
                    all_drive_scores_by_diff_count[diff_count][drive].append(score)
                    if drive == 'protection':
                        protection_scores_by_diff_count[diff_count].append(score)

        print(f"\n--- {agent} (n={n} aligned events) ---")

        if protection_scores_by_diff_count:
            print(f"\n  Protection score by diffTexts count:")
            for dc in sorted(protection_scores_by_diff_count):
                scores = protection_scores_by_diff_count[dc]
                avg = sum(scores) / len(scores)
                print(f"    diffTexts={dc}: avg={avg:.2f} (n={len(scores)})")

        print(f"\n  All drives by diffTexts count:")
        for dc in sorted(all_drive_scores_by_diff_count):
            drives = all_drive_scores_by_diff_count[dc]
            print(f"    diffTexts={dc}:")
            for drive in sorted(drives):
                scores = drives[drive]
                avg = sum(scores) / len(scores)
                print(f"      {drive}: avg={avg:.2f} (n={len(scores)})")


def print_per_event_detail(records_by_agent, classifier_results, agent_filter=None):
    """Print per-event detail for debugging."""
    events_by_agent = defaultdict(list)
    for result in classifier_results:
        agent = result.get('character', 'unknown')
        events_by_agent[agent].append(result)

    agents = [agent_filter] if agent_filter else sorted(records_by_agent)

    for agent in agents:
        records = records_by_agent.get(agent, [])
        events = events_by_agent.get(agent, [])
        n = min(len(records), len(events))

        print(f"\n=== {agent} — per-event detail ===\n")
        for i in range(n):
            rec = records[i]
            evt = events[i]
            asm = rec.get('assembly', {})
            appr = rec.get('appraisal')
            diff = rec.get('differentiation')

            desc_preview = evt.get('desc', '')[:80]
            print(f"Event {i}: {desc_preview}...")

            if appr:
                print(f"  Appraisal: rendered={appr['rendered']}, filtered={appr['filtered']}")
                print(f"    Situations: {[s['id'] + ':' + str(s['confidence']) for s in appr['situations']]}")
            else:
                print(f"  Appraisal: BYPASSED (classifier returned empty)")

            if diff:
                print(f"  Differentiation: total={diff['total']}, included={diff['included']}, excluded={diff['excluded']}")
                if diff['excluded_pairs']:
                    print(f"    Excluded pairs: {diff['excluded_pairs']}")

            print(f"  Assembly: drives={asm.get('drives', '?')} [{', '.join(asm.get('drive_names', []))}], "
                  f"diffTexts={asm.get('diff_texts', '?')}, totalParts={asm.get('total_parts', '?')}")

            scores = evt.get('scores', {})
            score_str = ', '.join(
                f"{d}={'R' if v.get('relevant') else '-'}{v.get('score', '-')}"
                for d, v in scores.items()
                if isinstance(v, dict)
            )
            print(f"  Scores: {score_str}")
            print()


def main():
    parser = argparse.ArgumentParser(description="Correlate instrumentation logs with classifier scores")
    parser.add_argument("server_log", help="Path to server log with instrumentation lines")
    parser.add_argument("classifier_json", nargs='?', help="Path to classifier results JSON")
    parser.add_argument("--log-only", action="store_true", help="Only parse and summarize instrumentation logs")
    parser.add_argument("--detail", "-d", help="Print per-event detail for this agent ID")
    parser.add_argument("--agent", "-a", help="Filter to this agent ID only")
    args = parser.parse_args()

    records_by_agent = parse_server_log(args.server_log)

    if not records_by_agent:
        print("ERROR: No instrumentation lines found in server log", file=sys.stderr)
        sys.exit(1)

    total = sum(len(v) for v in records_by_agent.values())
    print(f"Parsed {total} instrumentation records for {len(records_by_agent)} agents", file=sys.stderr)

    if args.agent:
        records_by_agent = {k: v for k, v in records_by_agent.items() if k == args.agent}

    print_instrumentation_summary(records_by_agent)

    if args.log_only or not args.classifier_json:
        return

    with open(args.classifier_json) as f:
        classifier_results = json.load(f)

    correlate_with_scores(records_by_agent, classifier_results)

    if args.detail:
        print_per_event_detail(records_by_agent, classifier_results, args.detail)


if __name__ == "__main__":
    main()
