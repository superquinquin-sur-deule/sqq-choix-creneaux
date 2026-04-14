-- V002__create_tables.sql

CREATE TABLE campaign (
    id              UUID PRIMARY KEY,
    status          VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    start_date      TIMESTAMP NOT NULL,
    end_date        TIMESTAMP,
    store_opening   DATE NOT NULL,
    week_a_reference DATE NOT NULL,
    CONSTRAINT chk_campaign_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE slot_template (
    id               UUID PRIMARY KEY,
    week             VARCHAR(1) NOT NULL,
    day_of_week      VARCHAR(9) NOT NULL,
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    min_capacity     INTEGER NOT NULL,
    max_capacity     INTEGER NOT NULL,
    odoo_template_id BIGINT,
    version          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_week CHECK (week IN ('A', 'B', 'C', 'D')),
    CONSTRAINT chk_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    CONSTRAINT chk_capacity CHECK (min_capacity > 0 AND max_capacity >= min_capacity)
);

CREATE TABLE cooperator (
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL UNIQUE,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    odoo_partner_id  BIGINT UNIQUE,
    keycloak_subject VARCHAR(255) UNIQUE
);

CREATE TABLE slot_registration (
    id               UUID PRIMARY KEY,
    slot_template_id UUID NOT NULL REFERENCES slot_template(id),
    cooperator_id    UUID NOT NULL UNIQUE REFERENCES cooperator(id),
    registered_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_registration_slot ON slot_registration(slot_template_id);

CREATE TABLE email_log (
    id            UUID PRIMARY KEY,
    cooperator_id UUID NOT NULL REFERENCES cooperator(id),
    type          VARCHAR(20) NOT NULL,
    sent_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_email_type CHECK (type IN ('CONFIRMATION', 'REMINDER'))
);
