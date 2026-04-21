DELETE FROM slot_registration WHERE cooperator_id IN (SELECT id FROM cooperator WHERE barcode_base IS NULL);
DELETE FROM email_log WHERE cooperator_id IN (SELECT id FROM cooperator WHERE barcode_base IS NULL);
DELETE FROM cooperator WHERE barcode_base IS NULL;
ALTER TABLE cooperator ALTER COLUMN barcode_base SET NOT NULL;
