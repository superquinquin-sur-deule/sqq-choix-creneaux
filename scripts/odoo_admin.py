#!/usr/bin/env python3
"""Pull créneaux/cooperators from Odoo, or create shift templates from a CSV.

Read path mirrors OdooXmlRpcClient.java so a developer can verify connectivity
and data shape without booting the Quarkus app. Write path creates
shift.template rows from a CSV for seeding a dev/staging Odoo instance.
"""
import argparse
import csv
import os
import sys
import unicodedata
from datetime import datetime, timedelta, timezone
from urllib.parse import urlparse
from zoneinfo import ZoneInfo

import odoorpc

PARIS_TZ = ZoneInfo("Europe/Paris")
ODOO_DT_FMT = "%Y-%m-%d %H:%M:%S"


SLOT_FIELDS = [
    "id",
    "week_name",
    "week_list",
    "start_datetime",
    "end_datetime",
    "worker_nb_min",
    "worker_nb_max",
]
SLOT_DOMAIN = [("active", "=", True)]

COOPERATOR_FIELDS = ["id", "email", "name", "working_state"]
COOPERATOR_DOMAIN = [
    ("is_member", "=", True),
    ("user_ids", "!=", False)
]
# Binômes are duplicated as child "contact" entries on the main coop's partner
# record, with the binôme's name. We collect those names to filter out the
# binômes' own top-level partner records.
BINOME_DOMAIN = [("parent_id", "!=", False), ("type", "=", "contact")]

CSV_REQUIRED_COLUMNS = ("week_list", "start_datetime", "end_datetime")
CSV_OPTIONAL_INT_COLUMNS = ("worker_nb_min", "worker_nb_max")
# Odoo computes these from start_datetime + coop_shift.week_a_date config.
# Accept them in CSV headers for readability, but do not send on create.
CSV_IGNORED_COLUMNS = ("week_name", "week_number")

SEATS_AVAILABILITY_VALUES = ("limited", "unlimited")

# shift.template.week_list is a Selection of 2-letter codes (coop_shift module).
# Accept the canonical code, numeric 0..6, or day names (en/fr) for convenience.
WEEK_LIST_ALIASES = {
    "mo": "MO", "monday": "MO", "lundi": "MO", "0": "MO",
    "tu": "TU", "tuesday": "TU", "mardi": "TU", "1": "TU",
    "we": "WE", "wednesday": "WE", "mercredi": "WE", "2": "WE",
    "th": "TH", "thursday": "TH", "jeudi": "TH", "3": "TH",
    "fr": "FR", "friday": "FR", "vendredi": "FR", "4": "FR",
    "sa": "SA", "saturday": "SA", "samedi": "SA", "5": "SA",
    "su": "SU", "sunday": "SU", "dimanche": "SU", "6": "SU",
}


def connect():
    url = os.environ.get("ODOO_URL", "http://localhost:8069")
    db = os.environ.get("ODOO_DB", "odoo")
    username = os.environ.get("ODOO_USERNAME", "admin")
    password = os.environ.get("ODOO_PASSWORD", "admin")

    parsed = urlparse(url)
    protocol = "jsonrpc+ssl" if parsed.scheme == "https" else "jsonrpc"
    port = parsed.port or (443 if parsed.scheme == "https" else 8069)
    host = parsed.hostname or "localhost"

    print(f"Connecting to {url} (db={db}, user={username})")
    odoo = odoorpc.ODOO(host, protocol=protocol, port=port)
    odoo.login(db, username, password)
    print(f"Authenticated as uid={odoo.env.uid}")
    return odoo


def pull_slot_templates(odoo):
    ids = odoo.env["shift.template"].search(SLOT_DOMAIN)
    records = odoo.env["shift.template"].read(ids, SLOT_FIELDS)
    print(f"\nPulled {len(records)} slot templates (shift.template)")
    for r in records:
        print(
            f"  id={r['id']} week_name={r.get('week_name')!r} "
            f"week_list={r.get('week_list')!r} "
            f"start={r.get('start_datetime')} end={r.get('end_datetime')} "
            f"min={r.get('worker_nb_min')} max={r.get('worker_nb_max')}"
        )
    return records


def _normalize_name(s):
    if not s:
        return ""
    nfd = unicodedata.normalize("NFD", s)
    no_accent = "".join(c for c in nfd if unicodedata.category(c) != "Mn")
    return " ".join(no_accent.lower().split())


def _levenshtein(a, b, threshold):
    la, lb = len(a), len(b)
    if abs(la - lb) > threshold:
        return threshold + 1
    prev = list(range(lb + 1))
    for i in range(1, la + 1):
        curr = [i] + [0] * lb
        row_min = curr[0]
        for j in range(1, lb + 1):
            cost = 0 if a[i - 1] == b[j - 1] else 1
            curr[j] = min(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            if curr[j] < row_min:
                row_min = curr[j]
        if row_min > threshold:
            return threshold + 1
        prev = curr
    return prev[lb]


def _matches_binome(name, normalized_binomes):
    n = _normalize_name(name)
    if not n:
        return False
    for bn in normalized_binomes:
        threshold = min(3, max(1, min(len(n), len(bn)) // 5))
        if _levenshtein(n, bn, threshold) <= threshold:
            return True
    return False


def pull_cooperators(odoo):
    binome_ids = odoo.env["res.partner"].search(BINOME_DOMAIN)
    normalized_binomes = {
        _normalize_name(r.get("name") or "")
        for r in odoo.env["res.partner"].read(binome_ids, ["name"])
    }
    normalized_binomes.discard("")

    ids = odoo.env["res.partner"].search(COOPERATOR_DOMAIN)
    all_records = odoo.env["res.partner"].read(ids, COOPERATOR_FIELDS)
    records = [r for r in all_records if not _matches_binome(r.get("name") or "", normalized_binomes)]
    skipped = len(all_records) - len(records)
    print(f"\nPulled {len(records)} cooperators (res.partner), skipped {skipped} binôme(s)")
    for r in records:
        print(
            f"  id={r['id']} name={r.get('name')!r} "
            f"email={r.get('email')!r} working_state={r.get('working_state')!r}"
        )
    return records


def _resolve_csv_paths(csv_arg, target):
    if target == "slots":
        return {"slots": csv_arg}
    if target == "cooperators":
        return {"cooperators": csv_arg}
    base, ext = os.path.splitext(csv_arg)
    ext = ext or ".csv"
    return {"slots": f"{base}-slots{ext}", "cooperators": f"{base}-cooperators{ext}"}


def export_records_to_csv(records, fields, path):
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        for r in records:
            writer.writerow({k: ("" if r.get(k) is None else r.get(k)) for k in fields})
    print(f"Exported {len(records)} record(s) to {path}")


def _row_to_values(row):
    values = {col: row[col].strip() for col in CSV_REQUIRED_COLUMNS}
    # CSV datetimes are Europe/Paris local time; Odoo persists Datetime fields
    # in UTC, so we must convert before sending — otherwise the UI shows the
    # values shifted by the Paris offset (the bug fix-template-tz cleans up).
    for dt_col in ("start_datetime", "end_datetime"):
        values[dt_col] = _shift_by(values[dt_col], _paris_offset_at(values[dt_col]))
    wl = values["week_list"].lower()
    if wl not in WEEK_LIST_ALIASES:
        raise ValueError(
            f"unknown week_list {values['week_list']!r}; "
            f"use MO/TU/WE/TH/FR/SA/SU, 0..6, or a day name (monday/lundi/...)"
        )
    values["week_list"] = WEEK_LIST_ALIASES[wl]
    for col in CSV_OPTIONAL_INT_COLUMNS:
        raw = (row.get(col) or "").strip()
        if raw:
            values[col] = int(raw)
    # worker_nb_min/max are custom fields; mirror into coop_shift's
    # seats_min/seats_max so the capacity shows up in Odoo's UI too.
    if "worker_nb_min" in values:
        values["seats_min"] = values["worker_nb_min"]
    if "worker_nb_max" in values:
        values["seats_max"] = values["worker_nb_max"]

    availability = (row.get("seats_availability") or "").strip().lower()
    if availability:
        if availability not in SEATS_AVAILABILITY_VALUES:
            raise ValueError(
                f"unknown seats_availability {availability!r}; "
                f"use 'limited' or 'unlimited'"
            )
        values["seats_availability"] = availability
    return values


def create_slots_from_csv(odoo, csv_path, dry_run=False):
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        if reader.fieldnames is None:
            raise ValueError(f"{csv_path} is empty")
        missing = set(CSV_REQUIRED_COLUMNS) - set(reader.fieldnames)
        if missing:
            raise ValueError(
                f"{csv_path} missing required columns: {sorted(missing)}"
            )
        rows = list(reader)

    print(f"\n{'Dry-run: would create' if dry_run else 'Creating'} "
          f"{len(rows)} shift templates from {csv_path}")
    created = []
    failed = 0
    for lineno, row in enumerate(rows, start=2):  # line 1 is the header
        try:
            values = _row_to_values(row)
        except (KeyError, ValueError) as e:
            print(f"  line {lineno}: SKIP invalid row: {e}", file=sys.stderr)
            failed += 1
            continue

        if dry_run:
            print(f"  line {lineno}: {values}")
            continue

        try:
            new_id = odoo.env["shift.template"].create(values)
        except Exception as e:
            print(f"  line {lineno}: FAILED {values}: {e}", file=sys.stderr)
            failed += 1
            continue

        created.append(new_id)
        print(f"  line {lineno}: created id={new_id} {values}")

    if dry_run:
        print(f"\nDry-run complete ({failed} invalid row(s))")
    else:
        print(f"\nCreated {len(created)}/{len(rows)} shift templates "
              f"({failed} failure(s))")
    return created, failed


def _paris_offset_at(naive_str):
    """Return the Europe/Paris UTC offset (timedelta) at the given naive datetime."""
    naive = datetime.strptime(naive_str, ODOO_DT_FMT)
    return naive.replace(tzinfo=PARIS_TZ).utcoffset()


def _shift_by(naive_str, delta):
    """Return naive 'YYYY-MM-DD HH:MM:SS' shifted by delta (timedelta)."""
    return (datetime.strptime(naive_str, ODOO_DT_FMT) - delta).strftime(ODOO_DT_FMT)


def fix_template_tz_from_csv(odoo, csv_path, apply=False):
    """Find shift.templates whose (week_list, start_datetime, end_datetime) match
    rows of the CSV (which holds Paris-local times stored as-if-UTC by a buggy
    create) and rewrite their datetimes to true UTC. Also rewrites derived
    shift.shift.date_begin/date_end the same way.
    """
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    print(f"\n{'Applying' if apply else 'Dry-run:'} tz fix from {csv_path} ({len(rows)} rows)")

    matched_template_ids = []
    skipped = 0
    not_found = 0
    multi = 0

    for lineno, row in enumerate(rows, start=2):
        wl_raw = (row.get("week_list") or "").strip().lower()
        wl = WEEK_LIST_ALIASES.get(wl_raw)
        start = (row.get("start_datetime") or "").strip()
        end = (row.get("end_datetime") or "").strip()
        if not (wl and start and end):
            print(f"  line {lineno}: SKIP missing fields", file=sys.stderr)
            skipped += 1
            continue

        domain = [
            ("week_list", "=", wl),
            ("start_datetime", "=", start),
            ("end_datetime", "=", end),
        ]
        ids = odoo.env["shift.template"].search(domain)
        if not ids:
            print(f"  line {lineno}: NOT FOUND week_list={wl} start={start}", file=sys.stderr)
            not_found += 1
            continue
        if len(ids) > 1:
            print(f"  line {lineno}: AMBIGUOUS {len(ids)} matches for week_list={wl} start={start} → {ids}", file=sys.stderr)
            multi += 1
            continue

        tid = ids[0]
        # Offset is computed from the template date (the moment the bug was
        # introduced). Derived shift.shift inherit the same offset — Odoo
        # projected them keeping the wrong Paris-displayed hour, so the
        # absolute UTC error is constant across the recurrence.
        offset = _paris_offset_at(start)
        new_start = _shift_by(start, offset)
        new_end = _shift_by(end, offset)
        print(f"  line {lineno}: template id={tid} (offset {offset}) "
              f"{start}→{new_start} | {end}→{new_end}")
        matched_template_ids.append((tid, offset, new_start, new_end))

    # Derived shift.shift records — apply each parent template's offset
    shift_updates = []
    offset_by_template = {tid: off for tid, off, _, _ in matched_template_ids}
    if matched_template_ids:
        derived = odoo.env["shift.shift"].search(
            [("shift_template_id", "in", list(offset_by_template))]
        )
        derived_recs = odoo.env["shift.shift"].read(derived, ["date_begin", "date_end", "shift_template_id"])
        print(f"\n  {len(derived_recs)} derived shift.shift to rewrite:")
        for r in derived_recs:
            parent_tid = r["shift_template_id"][0]
            off = offset_by_template[parent_tid]
            new_b = _shift_by(r["date_begin"], off)
            new_e = _shift_by(r["date_end"], off)
            print(f"    shift id={r['id']} (template {parent_tid}, offset {off}) "
                  f"{r['date_begin']}→{new_b} | {r['date_end']}→{new_e}")
            shift_updates.append((r["id"], new_b, new_e))

    print(f"\nSummary: matched={len(matched_template_ids)} not_found={not_found} "
          f"ambiguous={multi} skipped={skipped} derived_shifts={len(shift_updates)}")

    if not apply:
        print("(dry-run; rerun with --apply to write)")
        return 0

    print("\nApplying writes...")
    for tid, _, ns, ne in matched_template_ids:
        odoo.env["shift.template"].write([tid], {"start_datetime": ns, "end_datetime": ne})
    for sid, nb, ne in shift_updates:
        odoo.env["shift.shift"].write([sid], {"date_begin": nb, "date_end": ne})
    print(f"Wrote {len(matched_template_ids)} templates and {len(shift_updates)} shifts.")
    return 0


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_pull = sub.add_parser("pull", help="fetch records from Odoo")
    p_pull.add_argument(
        "target",
        nargs="?",
        default="all",
        choices=["slots", "cooperators", "all"],
        help="what to fetch (default: all)",
    )
    p_pull.add_argument(
        "--csv",
        metavar="PATH",
        help="also export pulled records to CSV file(s). With target=all, "
             "writes PATH-slots.csv and PATH-cooperators.csv",
    )

    p_create = sub.add_parser(
        "create-slots", help="create shift.template rows from a CSV"
    )
    p_create.add_argument("csv_path", help="path to CSV file")
    p_create.add_argument(
        "--dry-run",
        action="store_true",
        help="parse and validate the CSV without calling Odoo",
    )

    p_fix = sub.add_parser(
        "fix-template-tz",
        help="rewrite shift.template/shift.shift datetimes that were stored as "
             "Paris-local-time-as-UTC into true UTC (matches templates by CSV)",
    )
    p_fix.add_argument("csv_path", help="path to CSV used to seed the buggy templates")
    p_fix.add_argument(
        "--apply",
        action="store_true",
        help="actually write changes (default: dry-run)",
    )

    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)

    try:
        odoo = None if (args.cmd == "create-slots" and args.dry_run) else connect()
    except Exception as e:
        print(f"ERROR: failed to connect/authenticate: {e}", file=sys.stderr)
        return 1

    if args.cmd == "pull":
        csv_paths = _resolve_csv_paths(args.csv, args.target) if args.csv else {}
        if args.target in ("slots", "all"):
            try:
                slots = pull_slot_templates(odoo)
                if "slots" in csv_paths:
                    export_records_to_csv(slots, SLOT_FIELDS, csv_paths["slots"])
            except Exception as e:
                print(f"ERROR: failed to pull shift.template: {e}", file=sys.stderr)
                return 1
        if args.target in ("cooperators", "all"):
            try:
                cooperators = pull_cooperators(odoo)
                if "cooperators" in csv_paths:
                    export_records_to_csv(cooperators, COOPERATOR_FIELDS, csv_paths["cooperators"])
            except Exception as e:
                print(f"ERROR: failed to pull res.partner: {e}", file=sys.stderr)
                return 1
        return 0

    if args.cmd == "create-slots":
        try:
            _, failed = create_slots_from_csv(odoo, args.csv_path, dry_run=args.dry_run)
        except (OSError, ValueError) as e:
            print(f"ERROR: {e}", file=sys.stderr)
            return 1
        return 1 if failed else 0

    if args.cmd == "fix-template-tz":
        try:
            return fix_template_tz_from_csv(odoo, args.csv_path, apply=args.apply)
        except (OSError, ValueError) as e:
            print(f"ERROR: {e}", file=sys.stderr)
            return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
