-- Codes sent by email to confirm an address or reset a password: kept hashed, short-lived, few tries.
CREATE TABLE email_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- 'verify' or 'reset'.
    purpose TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, purpose)
);
