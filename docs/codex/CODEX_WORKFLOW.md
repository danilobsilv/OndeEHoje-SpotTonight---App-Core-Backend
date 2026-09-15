# Codex Workflow

Use Codex incrementally.

Do not ask Codex to "build the entire backend".

Recommended sequence:

```text
M0 Project Foundation
M1 Users + Authentication
M2 Categories
M3 Establishment Core
M4 Operating Hours
M5 Moderation
M6 Discovery
```

## Suggested Codex CLI prompt pattern

```text
Read AGENTS.md and all documentation relevant to milestone M<N>.
Implement only docs/codex/<MILESTONE_FILE>.md.
Do not implement later roadmap work.
Before changing code, summarize the existing architecture and the files you expect to modify.
After implementation:
1. run all required tests,
2. report changed files,
3. report architectural decisions made,
4. report any divergence from the docs,
5. do not proceed to the next milestone.
```

## Review loop

After each milestone:

1. inspect the diff;
2. inspect database migration;
3. inspect tests;
4. run the application;
5. manually exercise important endpoints;
6. update docs only if an intentional decision changed;
7. then create the next Codex task.

Treat Codex as an implementation agent operating inside an already-designed system, not as the product architect.
