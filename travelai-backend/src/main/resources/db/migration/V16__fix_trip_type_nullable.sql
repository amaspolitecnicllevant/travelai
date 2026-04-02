-- Make trip_type and budget nullable so the Trip entity (which doesn't have these fields) can insert without errors
ALTER TABLE trips ALTER COLUMN trip_type DROP NOT NULL;
ALTER TABLE trips ALTER COLUMN budget DROP NOT NULL;
