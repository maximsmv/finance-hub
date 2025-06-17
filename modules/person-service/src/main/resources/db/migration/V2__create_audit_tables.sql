CREATE SEQUENCE person.revinfo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE person.revinfo (
    rev INTEGER DEFAULT nextval('person.revinfo_seq'),
    revtstmp BIGINT,
    PRIMARY KEY ( rev )
);

CREATE TABLE person.countries_aud (
    id INTEGER NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE SMALLINT,
    created TIMESTAMP,
    updated TIMESTAMP,
    name VARCHAR(32),
    alpha2 VARCHAR(2),
    alpha3 VARCHAR(3),
    status VARCHAR(32),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_countries_aud_revinfo FOREIGN KEY (REV) REFERENCES person.revinfo(rev)
);

CREATE TABLE person.addresses_aud (
    id UUID NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE SMALLINT,
    created TIMESTAMP,
    updated TIMESTAMP,
    country_id INTEGER,
    address VARCHAR(128),
    zip_code VARCHAR(32),
    archived TIMESTAMP,
    city VARCHAR(32),
    state VARCHAR(32),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_addresses_aud_revinfo FOREIGN KEY (REV) REFERENCES person.revinfo(rev)
);

CREATE TABLE person.users_aud (
    id UUID NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE SMALLINT,
    secret_key VARCHAR(32),
    email VARCHAR(1024),
    created TIMESTAMP,
    updated TIMESTAMP,
    first_name VARCHAR(32),
    last_name VARCHAR(32),
    filled BOOLEAN,
    address_id UUID,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_users_aud_revinfo FOREIGN KEY (REV) REFERENCES person.revinfo(rev)
);

CREATE TABLE person.individuals_aud (
    id UUID NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE SMALLINT,
    user_id UUID,
    passport_number VARCHAR(32),
    phone_number VARCHAR(32),
    email VARCHAR(32),
    verified_at TIMESTAMP,
    archived_at TIMESTAMP,
    status VARCHAR(32),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_individuals_aud_revinfo FOREIGN KEY (REV) REFERENCES person.revinfo(rev)
);