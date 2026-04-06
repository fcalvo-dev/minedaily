# MineDaily — AGENTS.md

## Purpose
This repository contains the MineDaily project.
Use the documentation in `docs/` as the source of truth before proposing, changing, or reviewing code.

## Primary source of truth
Always read and follow these documents first:

- `docs/01-product-rules.md`
- `docs/02-domain-model.md`
- `docs/03-api-contract.md`
- `docs/04-delivery-plan.md`

If code and docs diverge, do not silently invent behavior.
Call out the mismatch and propose whether the code or the docs should change.

## Product rules that must not be violated
- The backend is the source of truth.
- The current daily challenge is shared by all users.
- Official timezone: `America/Argentina/Cordoba`.
- Daily rollover happens at `21:00` Córdoba time.
- Challenge generation must be deterministic and reproducible.
- Never expose `internalSeed` or real mine positions to the frontend.
- The frontend must only receive public challenge data and the visible board state for a session.
- A competitive session starts with 3 lives.
- `1 error = 1 mine stepped on = 1 life lost`.
- A session ends only when the player wins or runs out of lives.
- Leaderboard order is: `time ASC`, `errors ASC`, `clicks ASC`, then technical tie-breakers.

## Domain and naming guidance
Keep names aligned with the domain model documents.
Prefer these concepts and names unless there is a documented reason to change them:

- `DailyChallenge`
- `ChallengeBoardDefinition`
- `BoardGenerator`
- `GameSession`
- `LeaderboardEntry`
- `UserStats`

Do not introduce alternative names for the same concept without updating the docs.

## API guidance
Keep the API aligned with `docs/03-api-contract.md`.
Prefer server-authoritative gameplay.

Important expectations:
- `GET /api/challenges/current` returns only public challenge information.
- `POST /api/challenges/current/sessions` must not create multiple active sessions for the same user and challenge.
- Game action endpoints must apply business rules on the server.
- The API should return a session snapshot that is enough for the frontend to render the current visible state.
- Internal/dev endpoints must stay clearly separated from the public API.

## Delivery guidance
Work in small vertical slices following `docs/04-delivery-plan.md`.
Recommended implementation order:
1. current challenge provisioning and retrieval
2. current challenge status
3. session lifecycle
4. gameplay actions
5. leaderboard
6. frontend integration and hardening

## Engineering preferences
- Prefer simple, explicit designs over premature abstraction.
- Keep code consistent with the existing stack: Java 21, Spring Boot, PostgreSQL, Flyway, Spring Security, React, Vite, TypeScript.
- Add or update tests when changing business behavior.
- Avoid introducing hidden product decisions in code comments or implementation.
- If a new rule appears during implementation, propose a docs update first or alongside the code change.

## When working in this repo
Before substantial code changes:
1. identify which of the 4 docs govern the task
2. verify the implementation approach against those docs
3. mention any ambiguity explicitly

After substantial code changes:
1. summarize what changed
2. note which document sections were implemented
3. call out any remaining mismatch or open question

## Scope discipline
For the MVP, avoid adding features that are explicitly out of scope in the docs, such as:
- friends
- chat
- leagues
- replay system
- multiplayer real-time modes
- advanced anti-cheat

