# BACKLOG

## Product Direction

- Product type: `B2C website`
- Current priority: finish a working public website before Android
- Possible later expansion: Android app after stable web launch
- Target for first working web release: `2026-05-01`

## Release Goal: `v1 Working Website`

By the first release, the product should provide:

- stable MTProto proxy ingestion from multiple external sources
- automatic proxy checks with ranking based on reliability
- public website for desktop and mobile usage
- basic feedback loop from real users
- protection from simple abuse and overload
- minimal admin controls for operating the catalog
- observability sufficient for real operation

## Working Assumption for Launch

- Default execution assumption: `usable public beta`
- Meaning:
- site is public and usable by real users
- core flows work reliably enough for limited real traffic
- known non-critical gaps may remain after launch
- production safety basics must still exist: rate limiting, health checks, logging, admin access, rollback path
- If you want a stricter bar than public beta, that decision stays on moderation

## `v1` Launch Scope

### In Scope

- public landing and usage website for desktop and mobile web
- best-proxy selection and fast switching flow
- proxy pack copy flow for mobile users
- external feedback ingestion from site users
- at least `3` working proxy sources
- lifecycle checking and score-based ranking
- minimal admin controls for operations
- public API needed by the website
- production basics for monitoring and support

### Explicitly Out of Scope for `v1`

- Android application
- user accounts and authentication for end users
- paid plans or billing
- personalized recommendations
- broad third-party API productization
- advanced analytics stack beyond launch needs
- full moderation backoffice with complex roles

## Scope Policy

### Must Have for `v1`

- multi-source ingestion
- proxy deduplication and normalization
- scheduled lifecycle checks
- score/ranking based on checks and feedback
- public endpoint for best proxies
- usable B2C website UX
- mobile-friendly website
- feedback submission with abuse protection
- rate limiting for public endpoints
- admin controls for recheck and moderation
- metrics, health, logs
- deployment-ready documentation

### Should Have Soon After `v1`

- source quality analytics
- retention policy for history and feedback tables
- cache for public endpoints
- richer proxy detail pages or explanations
- better score calibration based on real user behavior
- staging environment

### Later

- Android app
- authenticated user accounts
- personalized proxy recommendations
- push-style proxy delivery
- broader API productization

## Delivery Plan

## Phase 1. Product Foundation

### Dates

- Start: `2026-04-03`
- Target finish: `2026-04-08`

### Goals

- freeze `v1` scope
- convert roadmap into executable backlog
- define release criteria
- clean up product and technical documentation

### Tasks

- [x] Create and maintain this backlog as the single working plan
- [x] Fix and rewrite `README.md` as product + developer documentation
- [x] Define `v1` scope as `must-have / should-have / later`
- [x] Document current architecture and domain boundaries
- [x] Identify launch-blocking technical debt
- [x] Define release criteria for `v1`

### AI-Agent-Accelerated Notes

- I can complete most of this phase directly without waiting on external input
- Expected duration with AI support: `1-2 working days`

### Needs User Moderation

- [x] Confirm what counts as `v1 launch`: usable public beta vs stricter production-ready release

### Current Architecture Boundaries

- `parser`
  Responsible for fetching raw proxies from external sources, normalizing them, and inserting new entries.
- `checker`
  Responsible for quick checks, deep MTProto probing, lifecycle scheduling, and persistence of check history.
- `proxy`
  Responsible for public listing, stats, best-proxy retrieval, feedback intake, and response mapping.
- `scoring`
  Responsible for calculating reliability score from status, history, latency, freshness, and feedback.
- `common.metrics`
  Responsible for service-level and proxy-level metrics exposure.
- `bootstrap`
  Responsible for startup import and initial checking.
- `static frontend`
  Responsible for public B2C website experience backed by `/api/proxies/*`.

### Launch-Blocking Technical Debt

- [ ] Replace single-source ingestion with multi-source ingestion
- [ ] Add public API hardening and rate limiting
- [ ] Add consistent public error handling and API documentation
- [ ] Upgrade website from demo-grade UX to launch-grade UX
- [ ] Add minimal protected admin surface
- [ ] Add clear deployment/runbook documentation
- [ ] Add production-oriented observability beyond current basic metrics
- [ ] Decide and implement retention policy for growing check history

### Release Criteria for `v1`

- [ ] Website supports the core public user flows without manual backend intervention
- [ ] At least `3` proxy sources import successfully in the target environment
- [ ] Proxy lifecycle checks run on schedule and update status/score correctly
- [ ] `GET /api/proxies/best` is stable enough for public website usage
- [ ] Feedback flow works and has basic abuse controls
- [ ] Public traffic is protected by rate limiting and basic operational safeguards
- [ ] Admin surface allows manual recheck and moderation actions
- [ ] Health, metrics, and logs are sufficient to diagnose incidents
- [ ] Documentation is sufficient for local setup and deployment
- [ ] No known launch-blocker remains open

## Phase 2. Data Quality and Catalog Reliability

### Dates

- Start: `2026-04-06`
- Target finish: `2026-04-13`

### Goals

- remove single-source dependency
- improve incoming data quality
- make source usefulness measurable

### Tasks

- [x] Add support for at least `3` configured proxy sources
- [x] Standardize source abstraction for easy extension
- [x] Strengthen normalization and deduplication across sources
- [x] Record source-level ingestion metrics
- [x] Record reject reasons for bad or malformed entries
- [ ] Review whether stale proxies should be archived or retired
- [ ] Review whether current import flow needs batching or partial retries

### Temporary Source Decision

- Temporary third source approved for `v1 beta`: `mtpro_xyz_pointer`
- Reason: faster delivery of multi-source ingestion
- Risk: pointer-based source is less stable and less transparent than a direct raw feed
- Follow-up: replace or supplement it with a direct independent upstream before or soon after launch

### AI-Agent-Accelerated Notes

- I can implement new source connectors, normalization rules, repository updates, metrics, and tests
- Expected duration with AI support: `3-4 working days`

### Needs User Moderation

- [ ] Approve specific external proxy sources if they have legal, trust, or reputation risk
- [ ] Approve policy for keeping or deleting stale/dead proxies

## Phase 3. Public API Hardening

### Dates

- Start: `2026-04-10`
- Target finish: `2026-04-17`

### Goals

- make public API safe for B2C traffic
- reduce overload and abuse risk
- improve operability and integration quality

### Tasks

- [ ] Standardize API response and error format
- [ ] Add OpenAPI/Swagger docs
- [ ] Add rate limiting to public endpoints
- [ ] Add versioning strategy for public API
- [ ] Add caching for hot public endpoints
- [ ] Add stricter validation on request payloads
- [ ] Add basic anti-abuse rules for feedback endpoint
- [ ] Review headers and reverse-proxy behavior for client fingerprinting
- [ ] Add tests for API edge cases and error responses

### AI-Agent-Accelerated Notes

- I can implement most of this directly in backend code and tests
- Expected duration with AI support: `3-4 working days`

### Needs User Moderation

- [ ] Decide whether public API remains open or starts with tighter restrictions
- [ ] Decide how visible/documented the API should be for external reuse

## Phase 4. B2C Website

### Dates

- Start: `2026-04-14`
- Target finish: `2026-04-24`

### Goals

- turn current static page into a real user-facing product
- make proxy usage understandable and fast for non-technical users
- deliver a strong mobile web experience

### Tasks

- [ ] Redesign current website UX around real user flows
- [ ] Keep desktop flow focused on trying one proxy at a time
- [ ] Keep mobile flow focused on quick proxy-pack copy/import
- [ ] Improve loading, empty, and error states
- [ ] Improve product copy and trust explanations
- [ ] Explain verification states in user language
- [ ] Make interface clearly production-grade, not demo-grade
- [ ] Verify responsive behavior on mobile and desktop
- [ ] Decide whether multilingual UI is needed in `v1`
- [ ] Add analytics hooks if needed for product learning

### User Flows to Support

- [ ] Get the best currently available proxy
- [ ] Move to the next proxy quickly if the first fails
- [ ] Copy a mobile-friendly proxy pack
- [ ] Understand confidence level of each proxy
- [ ] Submit quick feedback after trying a proxy

### AI-Agent-Accelerated Notes

- I can do the design iteration, frontend implementation, copy structure, and responsive fixes directly
- Expected duration with AI support: `4-5 working days`

### Needs User Moderation

- [ ] Approve brand direction, naming, domain, and tone of voice
- [ ] Decide whether `v1` UI should be Russian, English, or bilingual
- [ ] Approve how strongly score/ranking should be exposed to users

## Phase 5. Admin Controls and Operations

### Dates

- Start: `2026-04-20`
- Target finish: `2026-04-28`

### Goals

- operate the product without direct database intervention
- support manual moderation and quality control

### Tasks

- [ ] Add admin endpoints or UI for manual recheck
- [ ] Add blacklist/whitelist controls
- [ ] Add visibility into source health and recent import results
- [ ] Add visibility into deep-probe failure reasons
- [ ] Add operational stats for alive/verified/dead breakdown
- [ ] Add protected admin access strategy
- [ ] Add tests for admin operations where practical

### AI-Agent-Accelerated Notes

- I can implement the backend surface and basic admin UI quickly if scope stays minimal
- Expected duration with AI support: `2-3 working days`

### Needs User Moderation

- [ ] Decide whether `v1` needs a real admin UI or protected admin endpoints are enough
- [ ] Decide minimum authentication model for admin access

## Phase 6. Observability and Pre-Launch

### Dates

- Start: `2026-04-24`
- Target finish: `2026-05-01`

### Goals

- make the service supportable in production
- reduce launch risk
- prepare for first real users

### Tasks

- [ ] Expand metrics where current visibility is insufficient
- [ ] Add dashboards and alerting targets
- [ ] Validate health/readiness behavior
- [ ] Review structured logging needs
- [ ] Verify fresh-environment startup
- [ ] Review deployment configuration and secrets handling
- [ ] Create release checklist
- [ ] Create post-launch monitoring checklist

### AI-Agent-Accelerated Notes

- I can implement metrics/logging code, documentation, and launch checklists directly
- Expected duration with AI support: `2-3 working days`

### Needs User Moderation

- [ ] Approve deployment target and hosting model
- [ ] Decide whether staging is required before first launch
- [ ] Confirm expected early traffic and acceptable downtime risk

## Post-Launch Window

### Dates

- Start: `2026-05-01`
- Target finish: `2026-05-15`

### Goals

- learn from real usage
- tune ranking and UX
- prepare a clean base for later Android work

### Tasks

- [ ] Review real user behavior and proxy success patterns
- [ ] Calibrate scoring based on production data
- [ ] Improve UX based on observed friction
- [ ] Tune cache, rate limits, and scheduling frequencies
- [ ] Define Android prerequisites, but do not start Android implementation yet

## Working Execution Model

### I Proceed Without Moderation On

- documentation cleanup
- backlog maintenance
- architecture cleanup
- ingestion implementation
- scoring refinement
- backend hardening
- abuse protection implementation
- observability
- B2C website implementation
- testing
- refactoring needed for delivery

### I Escalate to User Moderation On

- product scope disputes
- questionable external source selection
- legal/reputation-sensitive decisions
- branding and public product copy direction
- API openness and access policy
- moderation policy for feedback abuse
- deployment and infrastructure choices that affect cost or risk

## Priority Order

- [ ] Freeze `v1` scope
- [ ] Build the backlog into executable work items
- [ ] Deliver multi-source ingestion
- [ ] Harden public API
- [ ] Rebuild the B2C website UX
- [ ] Add admin controls
- [ ] Complete observability and launch preparation
- [ ] Launch `v1`
- [ ] Optimize after first users

## Definition of Done for `v1`

- [ ] At least `3` sources ingest successfully
- [ ] Proxies are normalized, deduplicated, checked, and ranked automatically
- [ ] Public site works well on desktop and mobile
- [ ] Feedback loop works with basic abuse controls
- [ ] Public endpoints are rate-limited and documented
- [ ] Admin controls exist for manual operational intervention
- [ ] Metrics and health endpoints support production monitoring
- [ ] Project documentation supports deployment and maintenance
- [ ] Release checklist is completed

## Open Decisions

- [x] Confirm exact `v1 launch` quality bar
- [x] Approve initial external source list
- [ ] Decide stale proxy retention policy
- [ ] Decide public API openness
- [ ] Decide `v1` language strategy: Russian, English, or bilingual
- [ ] Decide admin access model
- [ ] Decide deployment target
- [ ] Decide whether staging is mandatory

## Moderation Queue

- [x] Confirm or override the working assumption that `v1` is a usable public beta, not a stricter production-complete release
- [x] Approve the initial external source set for Phase 2, including whether two feeds from one upstream count temporarily toward launch readiness
