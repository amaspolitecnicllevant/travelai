-- V18: afegir hora d'arribada, sortida i transport preferit al viatge
ALTER TABLE trips ADD COLUMN IF NOT EXISTS arrival_time   VARCHAR(5);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS departure_time VARCHAR(5);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS preferred_transport VARCHAR(20);
