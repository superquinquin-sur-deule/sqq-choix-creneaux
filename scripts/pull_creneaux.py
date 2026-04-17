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
from urllib.parse import urlparse

import odoorpc


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
COOPERATOR_DOMAIN = [("working_state", "!=", "blocked")]

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


def pull_cooperators(odoo):
    ids = odoo.env["res.partner"].search(COOPERATOR_DOMAIN)
    records = odoo.env["res.partner"].read(ids, COOPERATOR_FIELDS)
    print(f"\nPulled {len(records)} cooperators (res.partner)")
    for r in records:
        print(
            f"  id={r['id']} name={r.get('name')!r} "
            f"email={r.get('email')!r} working_state={r.get('working_state')!r}"
        )
    return records


def _row_to_values(row):
    values = {col: row[col].strip() for col in CSV_REQUIRED_COLUMNS}
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

    p_create = sub.add_parser(
        "create-slots", help="create shift.template rows from a CSV"
    )
    p_create.add_argument("csv_path", help="path to CSV file")
    p_create.add_argument(
        "--dry-run",
        action="store_true",
        help="parse and validate the CSV without calling Odoo",
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
        if args.target in ("slots", "all"):
            try:
                pull_slot_templates(odoo)
            except Exception as e:
                print(f"ERROR: failed to pull shift.template: {e}", file=sys.stderr)
                return 1
        if args.target in ("cooperators", "all"):
            try:
                pull_cooperators(odoo)
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

    return 0


if __name__ == "__main__":
    sys.exit(main())
