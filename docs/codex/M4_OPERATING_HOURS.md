# Codex Task — M4 Operating Hours

Implement weekly operating hours and open/closed calculation.

Read `AGENTS.md`, `DOMAIN_MODEL.md`, `DATABASE_DESIGN.md`,
`ESTABLISHMENT_LIFECYCLE.md`, and `TESTING_STRATEGY.md`.

## Deliverables

- OperatingHours entity;
- Flyway migration;
- repository;
- OperatingHoursService;
- DTOs;
- endpoint to replace an owner's establishment schedule;
- validation for intervals;
- support multiple intervals per day;
- support overnight intervals;
- injectable `Clock`;
- timezone-aware current operating status calculation;
- tests.

## Rules

- day of week follows ISO values 1 through 7;
- no schedule row means closed;
- `closesNextDay=true` represents an overnight interval;
- owner must own establishment;
- update operation should have replacement/idempotent semantics;
- do not implement holiday/special hours yet.

## Required tests

- normal interval;
- multiple intervals/day;
- opening boundary;
- closing boundary;
- overnight interval before midnight;
- overnight interval after midnight;
- previous-day lookup;
- closed day;
- timezone conversion;
- overlapping interval rejection;
- fixed Clock determinism.

## Acceptance criteria

- all tests pass;
- no use of server default timezone for business semantics;
- `./mvnw test` passes.
