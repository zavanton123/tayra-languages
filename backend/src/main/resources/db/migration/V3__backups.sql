-- Cloud backups: the library files a device uploaded, kept in object storage; the newest few per account.
CREATE TABLE backups (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    device_name TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    size_bytes BIGINT NOT NULL,
    object_key TEXT NOT NULL UNIQUE
);

CREATE INDEX backups_user ON backups(user_id, created_at);
