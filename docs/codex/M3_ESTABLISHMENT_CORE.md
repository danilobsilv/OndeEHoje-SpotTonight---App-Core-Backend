# Codex Task — M3 Establishment Core

Implement establishment creation and owner-facing core operations only.

Read `AGENTS.md`, `DOMAIN_MODEL.md`, `DATABASE_DESIGN.md`, `API_GUIDELINES.md`,
`ESTABLISHMENT_LIFECYCLE.md`, and ADR-004.

## Deliverables

- Establishment entity;
- EstablishmentStatus;
- optimistic-lock `version`;
- Flyway migration;
- repository;
- owner-facing service;
- owner-facing controller;
- DTOs and explicit mapper;
- create establishment endpoint;
- current user's establishment list;
- current user's establishment detail;
- allowed owner update flow;
- tests.

## Required invariants

- authenticated user becomes owner;
- request cannot set owner ID;
- new establishments always start `PENDING_REVIEW`;
- request cannot set moderation status;
- owner may only access/mutate own establishment through owner endpoints;
- another user's private resource should generally result in `404`;
- sensitive APPROVED-field restrictions must follow lifecycle documentation.

## Out of scope

- operating hours;
- moderation actions;
- public discovery;
- recommendation logic.

## Acceptance criteria

- create/list/detail/update flow works;
- ownership rules are tested;
- default state is tested;
- optimistic version exists;
- no JPA entities are exposed as API DTOs;
- `./mvnw test` passes.
