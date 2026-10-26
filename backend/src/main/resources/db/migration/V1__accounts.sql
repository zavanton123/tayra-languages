-- Accounts: who signs in, how, and what they have paid for.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    -- Null for an account made with Google or Apple only.
    password_hash TEXT,
    google_sub TEXT UNIQUE,
    apple_sub TEXT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    -- 'paddle', 'apple' or 'google', once anything was bought.
    subscription_source TEXT,
    paddle_customer_id TEXT UNIQUE,
    paddle_subscription_id TEXT,
    apple_original_transaction_id TEXT UNIQUE,
    google_purchase_token TEXT UNIQUE,
    subscription_status TEXT,
    subscription_active_until TIMESTAMPTZ
);

-- One per signed-in device: its refresh token, kept hashed, so a device can be listed and signed out.
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    device_id TEXT NOT NULL,
    device_name TEXT NOT NULL DEFAULT '',
    platform TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX refresh_tokens_user ON refresh_tokens(user_id);
