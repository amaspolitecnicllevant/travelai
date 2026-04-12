-- V19: afegir camp de tipus de viatge (comma-separated, ex: "CULTURAL,FAMILY")
ALTER TABLE trips ADD COLUMN IF NOT EXISTS trip_types VARCHAR(100);
