CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    type VARCHAR(10) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(10) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id)
);