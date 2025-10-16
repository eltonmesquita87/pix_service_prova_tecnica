-- Create wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Create pix_keys table
CREATE TABLE pix_keys (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    key_type VARCHAR(20) NOT NULL,
    key_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pix_keys_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT uk_pix_key_value UNIQUE (key_value)
);

CREATE INDEX idx_pix_keys_wallet_id ON pix_keys(wallet_id);
CREATE INDEX idx_pix_keys_key_value ON pix_keys(key_value);

-- Create ledger_entries table (immutable)
CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    end_to_end_id VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_entries_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_ledger_entries_wallet_created ON ledger_entries(wallet_id, created_at);
CREATE INDEX idx_ledger_entries_end_to_end_id ON ledger_entries(end_to_end_id);

-- Create pix_transfers table
CREATE TABLE pix_transfers (
    end_to_end_id VARCHAR(100) PRIMARY KEY,
    from_wallet_id BIGINT NOT NULL,
    to_wallet_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    rejected_at TIMESTAMP,
    CONSTRAINT fk_pix_transfers_from_wallet FOREIGN KEY (from_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_pix_transfers_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_pix_transfers_from_wallet ON pix_transfers(from_wallet_id);
CREATE INDEX idx_pix_transfers_to_wallet ON pix_transfers(to_wallet_id);
CREATE INDEX idx_pix_transfers_status ON pix_transfers(status);

-- Create idempotency_keys table
CREATE TABLE idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    scope VARCHAR(50) NOT NULL,
    key_value VARCHAR(255) NOT NULL,
    response TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_scope_key UNIQUE (scope, key_value)
);

CREATE INDEX idx_idempotency_keys_scope_key ON idempotency_keys(scope, key_value);

-- Create webhook_events table
CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    end_to_end_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_id UNIQUE (event_id)
);

CREATE INDEX idx_webhook_events_event_id ON webhook_events(event_id);
CREATE INDEX idx_webhook_events_end_to_end_id ON webhook_events(end_to_end_id);
