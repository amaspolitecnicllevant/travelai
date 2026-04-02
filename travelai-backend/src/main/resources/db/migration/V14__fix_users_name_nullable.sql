-- Make users.name nullable (User entity does not declare nullable=false)
-- Registration doesn't require a display name — username is the identifier
ALTER TABLE users ALTER COLUMN name DROP NOT NULL;
