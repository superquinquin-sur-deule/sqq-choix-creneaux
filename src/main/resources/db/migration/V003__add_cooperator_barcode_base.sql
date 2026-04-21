ALTER TABLE cooperator ADD COLUMN barcode_base VARCHAR(32) UNIQUE;
ALTER TABLE cooperator DROP COLUMN keycloak_subject;
