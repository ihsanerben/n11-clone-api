CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    label VARCHAR(80),
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
