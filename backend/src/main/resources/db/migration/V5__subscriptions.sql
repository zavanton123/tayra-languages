-- Subscriptions bought through Paddle, the App Store or Google Play, each as its provider last
-- reported it. An account may sync until the latest end among its subscriptions; the account's own
-- subscription columns are kept to that, and set by hand for an account with none of these.
CREATE TABLE subscriptions (
    -- 'paddle', 'apple' or 'google'.
    source TEXT NOT NULL,
    -- Paddle's subscription id, Apple's original transaction id, or Google's purchase token.
    external_id TEXT NOT NULL,
    -- Null until the purchase is tied to an account, and again once the account is deleted.
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    product TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,
    -- Until when it lets the account sync; null when it does not.
    active_until TIMESTAMPTZ,
    -- When the provider's latest event about it happened: an older event arriving late changes nothing.
    event_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (source, external_id)
);

CREATE INDEX subscriptions_user ON subscriptions(user_id);
