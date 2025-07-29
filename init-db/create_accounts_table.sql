CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    owner UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT DEFAULT 0
);