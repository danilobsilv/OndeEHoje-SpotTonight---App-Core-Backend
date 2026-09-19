CREATE TABLE establishments (
    id UUID PRIMARY KEY,

    owner_id UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES categories(id),

    name VARCHAR(150) NOT NULL,
    description TEXT,

    phone VARCHAR,
    website_url VARCHAR,
    instagram_handle VARCHAR,

    street VARCHAR NOT NULL,
    number VARCHAR,
    complement VARCHAR,
    neighborhood VARCHAR,
    city VARCHAR NOT NULL,
    state VARCHAR NOT NULL,
    postal_code VARCHAR,
    country_code CHAR(2) NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    time_zone VARCHAR NOT NULL,

    status VARCHAR NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_establishments_name_nonblank
        CHECK (name ~ '[^[:space:]]'),

    CONSTRAINT ck_establishments_street_nonblank
        CHECK (street ~ '[^[:space:]]'),

    CONSTRAINT ck_establishments_city_nonblank
        CHECK (city ~ '[^[:space:]]'),

    CONSTRAINT ck_establishments_state_nonblank
        CHECK (state ~ '[^[:space:]]'),

    CONSTRAINT ck_establishments_time_zone_nonblank
        CHECK (time_zone ~ '[^[:space:]]'),

    CONSTRAINT ck_establishments_country_code_canonical
        CHECK (country_code ~ '^[A-Z]{2}$'),

    CONSTRAINT ck_establishments_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT ck_establishments_longitude
        CHECK (longitude BETWEEN -180 AND 180),

    CONSTRAINT ck_establishments_status
        CHECK (
            status IN (
                'PENDING_REVIEW',
                'APPROVED',
                'REJECTED',
                'SUSPENDED'
            )
        )
);

CREATE INDEX idx_establishments_owner_id
    ON establishments(owner_id);

CREATE INDEX idx_establishments_status
    ON establishments(status);

CREATE INDEX idx_establishments_category_id
    ON establishments(category_id);

CREATE INDEX idx_establishments_status_category_id
    ON establishments(status, category_id);