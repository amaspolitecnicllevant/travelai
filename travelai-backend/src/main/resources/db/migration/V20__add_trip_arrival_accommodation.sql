-- V20: punt d'arribada i adreça d'allotjament
ALTER TABLE trips ADD COLUMN IF NOT EXISTS arrival_location      VARCHAR(255);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS accommodation_address VARCHAR(255);
