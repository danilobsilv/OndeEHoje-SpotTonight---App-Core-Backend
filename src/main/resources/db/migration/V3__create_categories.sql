CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_categories_name_nonblank CHECK (name ~ '[^[:space:]]'),
    CONSTRAINT ck_categories_slug_canonical CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

-- Canonical identities are identical in every environment.
INSERT INTO categories (id, name, slug, active, created_at) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Bar', 'bar', TRUE, '2026-09-17T00:00:00Z'),
    ('00000000-0000-4000-8000-000000000002', 'Restaurant', 'restaurant', TRUE, '2026-09-17T00:00:00Z'),
    ('00000000-0000-4000-8000-000000000003', 'Café', 'cafe', TRUE, '2026-09-17T00:00:00Z'),
    ('00000000-0000-4000-8000-000000000004', 'Nightclub', 'nightclub', TRUE, '2026-09-17T00:00:00Z'),
    ('00000000-0000-4000-8000-000000000005', 'Pub', 'pub', TRUE, '2026-09-17T00:00:00Z');
