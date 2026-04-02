-- Change ip_address columns from inet to varchar — Hibernate cannot cast String to PostgreSQL inet type
ALTER TABLE audit_logs ALTER COLUMN ip_address TYPE VARCHAR(45);
ALTER TABLE consent_logs ALTER COLUMN ip_address TYPE VARCHAR(45);
