# Task Plan: SOH3IL 360 Remediation

## Goal
Implement the 19 reviewed audit findings authorized by the owner on 2026-11-15, with failing behavioral tests and local verification. Preserve locked UI contracts; C01 color changes and separate product decisions are not authorized.

## Next Step
Approved F01-F19 repairs are implemented and all four mandatory gates passed on the exact final source (134 tests, no failures/errors/skips). Owner authorized push and release; main and annotated tag v1.4.1 were pushed at a1e9515. Owner subsequently explicitly requested stopping GitHub checks. Do not query, watch or modify GitHub until authorized again. Final CI conclusions, published release and new APK signatures remain unverified. Local v1.4.1 gates passed; C01, very-short-viewport layout and physical-device execution remain separate scope.

## Current Phase
Phase 19

## Phases

### Phase 1: Requirements & Discovery
- [x] Understand user intent
- [x] Identify constraints
- [x] Document in findings.md
- **Status:** complete

### Phase 2: Planning & Structure
- [x] Define implementation order
- [x] Define regression and release gates
- **Status:** complete

### Phase 3: Core Correctness and Reliability
- [x] Canonical Jalali calculations and recurrence
- [x] Alarm/notification lifecycle and midnight freshness
- [x] Room migration and converter safety
- **Status:** complete

### Phase 4: Data, ICS and updater hardening
- [x] Harden bounded ICS parsing and timezone handling
- [x] Remove executable APK transport and keep strict release metadata URL policy
- **Status:** complete

### Phase 5: Material 3 and Persian/RTL UX
- [x] Upgrade Compose BOM/Material 3 safely
- [x] Fix semantic colors, touch targets, digits, font scaling and stale date UI
- **Status:** complete

### Phase 6: Modular boundaries and test coverage
- [x] Reduce unused infrastructure coupling from feature UI
- [x] Add unit and regression coverage for recurrence, ICS and URL policy
- **Status:** complete

### Phase 7: Testing and release verification
- [x] Run required Gradle gates using an isolated writable Gradle home
- [x] Review diff, security, and generated artifacts
- **Status:** complete

### Phase 8: Delivery
- [x] Report implemented, tested, and remaining items
- **Status:** complete

### Phase 9: Google harmful-app surface remediation
- [x] Remove custom APK self-update and `REQUEST_INSTALL_PACKAGES`
- [x] Remove direct Doze-exemption request and keep user-controlled battery settings
- [x] Re-scope FileProvider and validate manifest/source surfaces
- **Status:** complete

### Phase 10: Final delivery
- [x] Report implementation, verification, and external validation limits
- **Status:** complete

### Phase 11: v1.3.5 regression fixes
- [x] Correct widget measurement, holiday classification and in-app pinning/discoverability
- [x] Correct contact birthday projection and contain provider access errors
- [x] Preserve calendar and reminder draft state across Activity recreation
- [x] Bound ICS bytes before decoding and preserve Gregorian monthly/yearly recurrence
- **Status:** complete

### Phase 12: v1.3.5 verification and release
- [x] Run core, module, lint, debug and signed release build gates
- [x] Verify widget/state behavior on the connected device where available
- [x] Review the final diff, increment version, commit and fast-forward main
- [x] Push main/tag and verify CI plus four signed release assets
- **Status:** complete

### Phase 13: Reference research and remaining work
- [x] Inspect pinned Persian Calendar widget, configuration, update and module sources
- [x] Compare against current SOH3IL sources and record evidence boundaries
- [x] Deliver the comparison, prioritized remaining work and accurate README attribution
- **Status:** complete

### Phase 14: Independent widget design
- [x] Record the user's approval of independent implementation with MIT retained
- [x] Present the concrete widget design
- [x] Obtain design approval (2026-11-15: implement all items)
- [x] Write the implementation plan after design approval
- **Status:** complete

### Phase 15: Widget implementation and verification
- [x] Implement the approved design in tested slices without importing upstream code or assets
- [x] Run mandatory local gates; physical device checks explicitly waived by owner
- **Status:** complete

### Phase 16: Rollover debugging and delivery
- [x] Reproduce and fix notification rollover and recovery without opening the Activity
- [x] Test notification, widget and calendar boundaries locally; physical device tests explicitly waived by owner
- [x] Verify published CI/assets and report concrete results and limitations
- **Status:** complete

### Phase 17: New report-only 360 audit (2026-11-15)
- [x] Confirm report and repair plan before code changes
- [x] Review current source and run isolated core baseline plus temporary regression probes
- [x] Record Android gate blockers and untested device/security/performance boundaries
- [x] Deliver 19 prioritized findings, a separate design conflict and a repair plan
- **Status:** report ready; repairs subsequently approved and verified in Phase 18

### Phase 18: Approved audit repairs (2026-11-15)
- [x] F19 debug/release signing isolation and usable test harness
- [x] F04/F06-F09 recurrence and ICS regressions
- [x] F01-F03 alarm identity, snooze and failed delivery
- [x] F05/F14/F15 agenda, draft and import preservation
- [x] F10-F13/F17/F18 settings, updater and navigation
- [x] F16 faithful device-calendar output
- [x] Required Gradle gates and independent self-review (owner forbids subagents)
- **Status:** complete — approved repairs and local verification; no release

### Phase 19: Authorized push and v1.4.1 release (2026-11-16)
- [x] Confirm current remote baseline and release workflow
- [x] Bump patch to 1.4.1 and build to 28
- [x] Re-run local required gates for release source
- [x] Commit scoped repairs, push main and version tag
- [ ] Verify both CI conclusions and four published signed APKs
- **Status:** paused at owner request — GitHub verification stopped; final release status unverified

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Work in the existing checkout because Git ref creation is unavailable in this sandbox | User approved implementation; filesystem permits source writes but `.git` is read-only |
| Preserve the explicit Persian/RTL invariants from AGENTS.md | They are repository-level product contracts |
| Fix correctness and data loss before visual modernization | Calendar and reminder failures have higher user impact than styling drift |

## Errors Encountered
| Error | Resolution |
|-------|------------|
| `git switch -c codex/soh3il-360-remediation` could not create `.git/refs/heads` | Continue in the current clean checkout and keep the change set scoped; do not alter Git metadata |
| Removing Android backup exclusions was rejected by auto-review as sensitive cloud export | Preserve the existing privacy exclusions and defer the backup policy decision |
