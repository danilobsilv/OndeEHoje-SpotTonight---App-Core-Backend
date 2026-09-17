# ADR-001 — Use a Spring MVC Modular Monolith

## Status

Accepted.

## Context

The project needs a production-oriented core backend but does not currently have scale or organizational requirements that justify microservices.

## Decision

Use one Spring Boot deployable with Spring MVC.

Organize code by feature, with controller/service/repository/model/DTO layers inside each feature.

## Consequences

Positive:

- simple deployment;
- local transactions;
- easier debugging;
- clear MVC structure;
- future feature boundaries remain visible.

Trade-offs:

- all core modules scale together;
- a single process remains a failure domain;
- internal boundaries require discipline rather than network isolation.

Microservices may be reconsidered only when a real requirement justifies extraction.
