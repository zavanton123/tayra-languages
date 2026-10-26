-- Synced records: the newest merged version of each record of each account, and the account's
-- change number, which orders every accepted change so devices pull what came after their last pull.
CREATE TABLE records (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    key TEXT NOT NULL,
    seq BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    device_id TEXT NOT NULL DEFAULT '',
    -- The record's fields as the devices wrote them; text, not jsonb, so a record reads back exactly as sent.
    data TEXT,
    PRIMARY KEY (user_id, kind, key)
);

CREATE INDEX records_user_seq ON records(user_id, seq);

CREATE TABLE user_seq (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_seq BIGINT NOT NULL DEFAULT 0
);
