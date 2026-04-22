-- Deduplicate slot_template rows that share the same odoo_template_id.
-- Caused by sync minting a fresh UUID for each pulled slot, which bypassed the
-- upsert-by-PK path and inserted a new row on every sync run.

CREATE TEMP TABLE _slot_dedupe ON COMMIT DROP AS
SELECT t.id,
       t.odoo_template_id,
       ROW_NUMBER() OVER (
           PARTITION BY t.odoo_template_id
           ORDER BY (SELECT COUNT(*) FROM slot_registration r WHERE r.slot_template_id = t.id) DESC,
                    t.id
       ) AS rn
FROM slot_template t
WHERE t.odoo_template_id IS NOT NULL;

-- Drop registrations on losing rows that would conflict with an existing
-- registration for the same cooperator (slot_registration.cooperator_id is UNIQUE).
DELETE FROM slot_registration sr
USING _slot_dedupe d
WHERE sr.slot_template_id = d.id
  AND d.rn > 1
  AND EXISTS (
      SELECT 1 FROM slot_registration sr2
      WHERE sr2.cooperator_id = sr.cooperator_id
        AND sr2.id <> sr.id
  );

-- Re-point remaining registrations from losing rows to the keeper.
UPDATE slot_registration sr
SET slot_template_id = keeper.id
FROM _slot_dedupe loser
JOIN _slot_dedupe keeper
    ON keeper.odoo_template_id = loser.odoo_template_id
   AND keeper.rn = 1
WHERE sr.slot_template_id = loser.id
  AND loser.rn > 1;

-- Delete duplicate slot_template rows.
DELETE FROM slot_template t
USING _slot_dedupe d
WHERE t.id = d.id AND d.rn > 1;

-- Prevent future duplicates.
CREATE UNIQUE INDEX uk_slot_template_odoo_template_id
    ON slot_template (odoo_template_id)
    WHERE odoo_template_id IS NOT NULL;
