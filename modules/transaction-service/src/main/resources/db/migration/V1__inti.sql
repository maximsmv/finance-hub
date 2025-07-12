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

CREATE TABLE transfer_operations (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_uid UUID NOT NULL
);

CREATE TABLE payment_requests (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    user_uid UUID NOT NULL,
    wallet_uid UUID NOT NULL REFERENCES wallets(uid),
    amount DECIMAL(20, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(256),
    fee DECIMAL(20, 2),
    target_wallet_uid UUID,
    transaction_uid UUID,
    transfer_operation_uid UUID REFERENCES transfer_operations(uid),
    failure_reason VARCHAR(256),
    expires_at TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_payment_requests_wallet_uid ON payment_requests(wallet_uid);
CREATE INDEX idx_payment_requests_user_uid ON payment_requests(user_uid);
CREATE UNIQUE INDEX uq_payment_requests_transaction_uid ON payment_requests(transaction_uid);
CREATE UNIQUE INDEX uq_transfer_operations_transaction_uid ON transfer_operations(transaction_uid);