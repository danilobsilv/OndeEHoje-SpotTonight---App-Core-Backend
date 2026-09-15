# Codex Task — M6 Discovery

Implement the first public establishment discovery API.

Read `AGENTS.md`, `API_GUIDELINES.md`, `DOMAIN_MODEL.md`,
`ESTABLISHMENT_LIFECYCLE.md`, and `ARCHITECTURE.md`.

## Deliverables

- public EstablishmentController;
- EstablishmentDiscoveryService;
- approved-only public queries;
- public list;
- public detail;
- category filtering;
- basic name search;
- pagination;
- allowlisted sorting;
- open-now projection/filter where feasible;
- latitude/longitude input validation;
- basic distance calculation;
- radius filtering appropriate for V1;
- tests.

## Public visibility invariant

Only `APPROVED` establishments may be returned.

Requests for known but non-public establishments through public detail must return `404`.

## Location validation

- latitude and longitude are supplied together;
- radius requires coordinates;
- latitude range: -90..90;
- longitude range: -180..180.

## Constraints

- do not add PostGIS unless explicitly instructed;
- do not add Elasticsearch/OpenSearch;
- do not implement recommendations;
- keep implementation appropriate for a small academic V1 dataset.

## Acceptance criteria

Test:

- approved listing;
- hidden pending/rejected/suspended resources;
- public detail visibility;
- category filtering;
- pagination bounds;
- search behavior;
- coordinate validation;
- distance/radius behavior;
- `./mvnw test` passes.
