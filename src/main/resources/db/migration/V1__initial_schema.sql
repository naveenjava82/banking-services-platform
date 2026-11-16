CREATE TABLE transactions (
 id UUID PRIMARY KEY,
 customer_id VARCHAR(100) NOT NULL,
 source VARCHAR(80) NOT NULL,
 source_id VARCHAR(100) NOT NULL,
 occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 amount NUMERIC(19,2) NOT NULL,
 currency VARCHAR(3) NOT NULL,
 description VARCHAR(500) NOT NULL,
 merchant VARCHAR(200) NOT NULL,
 category VARCHAR(40) NOT NULL,
 CONSTRAINT uq_transaction_source UNIQUE (customer_id, source, source_id)
);
CREATE INDEX ix_transactions_customer_time ON transactions(customer_id, occurred_at);
CREATE INDEX ix_transactions_customer_category ON transactions(customer_id, category);
CREATE TABLE fraud_findings (
 id UUID PRIMARY KEY,
 transaction_id UUID NOT NULL REFERENCES transactions(id),
 customer_id VARCHAR(100) NOT NULL,
 rule_code VARCHAR(80) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_fraud_rule UNIQUE(transaction_id, rule_code)
);
CREATE INDEX ix_fraud_customer ON fraud_findings(customer_id);
CREATE TABLE statements (
 id UUID PRIMARY KEY,
 customer_id VARCHAR(100) NOT NULL,
 original_filename VARCHAR(255) NOT NULL,
 storage_filename VARCHAR(100) NOT NULL UNIQUE,
 uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
 file_size BIGINT NOT NULL,
 sha256 VARCHAR(64) NOT NULL
);
CREATE INDEX ix_statements_customer ON statements(customer_id);
