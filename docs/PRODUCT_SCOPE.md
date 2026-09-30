# Product Scope — SpotNight

## Product idea

**SpotNight** is a backend-first location-aware platform for discovering establishments.

The primary user problem is:

> I want to know what is available right now, where it is, how far away it is, and until when it remains open.

Later, the platform will also recommend establishments based on user preferences and interactions.

## V1 focus

V1 focuses on the core Java/Spring backend.

### Included

- user registration;
- authentication;
- authorization;
- current user profile;
- establishment registration by authenticated users;
- establishment ownership;
- establishment moderation;
- admin approval/rejection;
- suspension/reactivation;
- categories;
- address and geographic coordinates;
- weekly operating hours;
- overnight operating intervals;
- public establishment listing/details;
- filtering and pagination;
- basic public discovery;
- PostgreSQL persistence;
- API documentation;
- automated tests.

### Deferred

- recommendation algorithms;
- Python/FastAPI recommendation service implementation;
- user taste/preferences;
- favorites;
- behavioral interaction events;
- reviews/ratings;
- chatbot;
- RAG;
- embeddings;
- vector databases;
- Redis;
- queues;
- Kafka/RabbitMQ;
- Elasticsearch/OpenSearch;
- Kubernetes;
- horizontal scaling;
- multi-region/high-availability architecture;
- merchant staff/multiple managers;
- special holiday hours;
- establishment deletion/archive flow;
- rich owner portal/frontend.

## Actors

### User

A normal authenticated account.

A user can:

- manage their own account;
- register zero or more establishments;
- view their submitted establishments;
- edit establishments according to lifecycle rules;
- resubmit rejected establishments;
- consume public discovery APIs.

### Admin

An internal privileged account.

An admin can:

- inspect submitted establishments;
- approve establishments;
- reject establishments with a reason;
- suspend approved establishments;
- reactivate suspended establishments;
- inspect moderation history.

There is intentionally no global `ESTABLISHMENT_OWNER` role in V1. Ownership is represented by the relationship between a user and an establishment.

## Frontend

The frontend is out of scope for this repository.

A future interface may be generated or prototyped with Google Stitch or another frontend stack. The backend API must remain client-independent.

## Recommendation system

Recommendations are a later milestone in a separate repository using Python/FastAPI.

The Java backend will remain the business-domain owner and will later send candidate establishments/features to the recommendation service.

The recommendation service must not directly mutate Spring-owned domain data.
