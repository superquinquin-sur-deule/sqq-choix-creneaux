# scripts

Developer utilities.

## pull_creneaux.py

Two modes against Odoo:

- **pull** — mirrors `OdooXmlRpcClient.java`: fetches `shift.template` and/or
  `res.partner` with the same domain and fields as the Java client.
- **create-slots** — creates `shift.template` rows from a CSV (handy for
  seeding dev/staging Odoo).

### Usage

The launcher creates/reuses the venv and installs requirements as needed:

```sh
export ODOO_URL=https://odoo.example.com
export ODOO_DB=mydb
export ODOO_USERNAME=admin
export ODOO_PASSWORD=secret

./scripts/pull_creneaux.sh pull                      # both (default)
./scripts/pull_creneaux.sh pull slots                # only shift.template
./scripts/pull_creneaux.sh pull cooperators          # only res.partner
./scripts/pull_creneaux.sh create-slots shifts.csv   # create from CSV
./scripts/pull_creneaux.sh create-slots shifts.csv --dry-run  # validate only
```

### CSV format (create-slots)

Columns — required: `week_list`, `start_datetime`, `end_datetime`; optional:
`worker_nb_min`, `worker_nb_max`, `seats_availability` (`limited` or
`unlimited`). See `example_shifts.csv` for a ready-to-use sample.

`worker_nb_min`/`worker_nb_max` are mirrored into `coop_shift`'s
`seats_min`/`seats_max` on create. To have Odoo actually enforce the cap,
set `seats_availability=limited`.

`week_list` accepts `MO`/`TU`/`WE`/`TH`/`FR`/`SA`/`SU` (the canonical
`coop_shift` selection values), `0`..`6`, or a day name
(`monday`/`lundi`/…). The script normalizes all forms to the 2-letter code.

`week_name` is a computed field in `coop_shift` (derived from
`start_datetime` + the `coop_shift.week_a_date` config parameter), so it
cannot be set on create. Extra columns in the CSV are ignored.

```csv
week_list,start_datetime,end_datetime,worker_nb_min,worker_nb_max
MO,2026-01-05 08:00:00,2026-01-05 10:45:00,3,8
TU,2026-01-13 10:45:00,2026-01-13 13:30:00,,
```

Or run the Python directly if you manage the venv yourself:

```sh
python3 -m venv scripts/.venv
scripts/.venv/bin/pip install -r scripts/requirements.txt
scripts/.venv/bin/python scripts/pull_creneaux.py
```

Defaults match `application.properties`: `http://localhost:8069`, db `odoo`,
user `admin`, password `admin`.

Exit code is `0` on success, `1` on auth or query failure.
