# Establishment Lifecycle

## States

```text
PENDING_REVIEW
APPROVED
REJECTED
SUSPENDED
```

## State machine

```text
                         approve
PENDING_REVIEW --------------------------> APPROVED
      |                                      |
      | reject                               | suspend
      v                                      v
   REJECTED                              SUSPENDED
      |                                      |
      | resubmit                             | reactivate
      v                                      |
PENDING_REVIEW                         APPROVED
```

## Creation

When an authenticated user submits an establishment:

```text
owner = authenticated user
status = PENDING_REVIEW
```

The client cannot select either field.

## Approval

Only an admin may approve.

Valid transition:

```text
PENDING_REVIEW -> APPROVED
```

Approval must:

1. validate current status;
2. update status;
3. append moderation event;
4. commit atomically.

## Rejection

Only an admin may reject.

Valid transition:

```text
PENDING_REVIEW -> REJECTED
```

A reason is required.

## Resubmission

Owner may edit a rejected establishment and resubmit.

Valid transition:

```text
REJECTED -> PENDING_REVIEW
```

## Suspension

Admin may suspend an approved establishment.

Valid transition:

```text
APPROVED -> SUSPENDED
```

Suspended establishments are immediately excluded from public discovery.

## Reactivation

Admin may reactivate a suspended establishment.

Valid transition:

```text
SUSPENDED -> APPROVED
```

## Public visibility

Only:

```text
APPROVED
```

is visible through public discovery endpoints.

Public requests for non-public establishments should generally return `404`.

## Editing rules

### PENDING_REVIEW / REJECTED

Owner may edit all owner-editable submission fields.

### APPROVED

V1 allows editing operational/non-identity information such as:

- description;
- phone;
- website;
- Instagram;
- operating hours.

Sensitive identity changes are deferred to a future re-review/change-request workflow.

Examples of sensitive fields:

- establishment name;
- primary category;
- address;
- coordinates.

## Audit history

Every admin moderation action produces an `EstablishmentModerationEvent`.

The current status remains on the establishment for efficient reads.

## Concurrency

Establishment uses optimistic locking.

If two concurrent requests modify the same stale version, the later conflicting operation must fail rather than overwrite the previous change.
