# Codex Task — M2 Categories

Implement only the Category milestone.

Read `AGENTS.md`, `DOMAIN_MODEL.md`, `DATABASE_DESIGN.md`, and `API_GUIDELINES.md`.

## Deliverables

- Category entity/model;
- Category repository;
- Category service;
- public category controller;
- Flyway migration;
- initial category seed data;
- `GET /api/v1/categories`;
- only active categories returned;
- tests.

## Rules

- Category is database-backed, not a Java enum.
- `slug` is unique.
- Category administration CRUD is out of scope.
- Do not add tags yet.

## Acceptance criteria

- migration creates/seeds categories;
- public endpoint returns active categories;
- inactive categories are excluded;
- duplicate slug is protected by DB constraint;
- `./mvnw test` passes.
