-- Add budget_level column (budget already exists from V1, nullable from V16)
ALTER TABLE trips ADD COLUMN IF NOT EXISTS budget_level VARCHAR(20);
