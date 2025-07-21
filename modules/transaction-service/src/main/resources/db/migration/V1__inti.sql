CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE wallet_types (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(18) NOT NULL,
    archived_at TIMESTAMP,
    user_type VARCHAR(15),
    creator VARCHAR(255),
    modifier VARCHAR(255)
);

CREATE TABLE wallets (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    wallet_type_uid UUID NOT NULL REFERENCES wallet_types(uid),
    user_uid UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
    archived_at TIMESTAMP
);

CREATE TABLE transactions (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    user_uid UUID NOT NULL,
    wallet_uid UUID NOT NULL REFERENCES wallets(uid),
    amount DECIMAL(20, 2) NOT NULL,
    fee DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(256),
    target_wallet_uid UUID,
    failure_reason VARCHAR(256)
);

CREATE INDEX idx_transactions_wallet_uid ON transactions(wallet_uid);
CREATE INDEX idx_transactions_user_uid ON transactions(user_uid);