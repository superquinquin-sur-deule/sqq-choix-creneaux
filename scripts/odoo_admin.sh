#!/usr/bin/env bash
# Create the venv if missing, install/sync requirements, then run odoo_admin.py.
# Forwards any args to the Python script. Env vars (ODOO_URL, ODOO_DB, ...) are inherited.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV="$SCRIPT_DIR/.venv"
REQS="$SCRIPT_DIR/requirements.txt"
STAMP="$VENV/.requirements.sha256"

if [[ ! -x "$VENV/bin/python" ]]; then
    echo "Creating venv at $VENV"
    python3 -m venv "$VENV"
fi

CURRENT_HASH="$(sha256sum "$REQS" | awk '{print $1}')"
if [[ ! -f "$STAMP" ]] || [[ "$(cat "$STAMP")" != "$CURRENT_HASH" ]]; then
    echo "Installing requirements"
    "$VENV/bin/pip" install --quiet --upgrade pip
    "$VENV/bin/pip" install --quiet -r "$REQS"
    echo "$CURRENT_HASH" > "$STAMP"
fi

exec "$VENV/bin/python" "$SCRIPT_DIR/odoo_admin.py" "$@"
