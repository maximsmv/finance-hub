CREATE SCHEMA IF NOT EXISTS person;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE person.countries (
    id SERIAL PRIMARY KEY,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP,
    name VARCHAR(64),
    alpha2 VARCHAR(2),
    alpha3 VARCHAR(3),
    status VARCHAR(32)
);

CREATE TABLE person.addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP,
    country_id INTEGER REFERENCES person.countries (id),
    address VARCHAR(128),
    zip_code VARCHAR(64),
    archived TIMESTAMP NOT NULL,
    city VARCHAR(64),
    state VARCHAR(64)
);

CREATE TABLE person.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    secret_key VARCHAR(32),
    email VARCHAR(1024),
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP,
    first_name VARCHAR(64),
    last_name VARCHAR(64),
    filled BOOLEAN,
    address_id UUID REFERENCES person.addresses(id)
);

CREATE TABLE person.individuals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES person.users(id),
    passport_number VARCHAR(32),
    phone_number VARCHAR(32),
    verified_at TIMESTAMP NOT NULL,
    archived_at TIMESTAMP,
    status VARCHAR(32)
);

CREATE UNIQUE INDEX idx_individuals_user_id_not_archived
    ON person.individuals (user_id)
    WHERE archived_at IS NULL;