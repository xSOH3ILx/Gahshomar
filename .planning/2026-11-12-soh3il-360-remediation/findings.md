# Findings & Decisions

## Requirements
- Preserve Kotlin-only source and modular boundaries.
- Keep the locked swipe direction, header vectors, FAB position, checkmark removal and selected-holiday white text contract.
- Complete the approved correctness, reliability, data, security, Material 3 and UX remediation.
- Do not claim tests or builds passed when the environment blocks them.

## Research Findings
- `JalaliDate.isLeap` mixes lookup-table and astronomical paths.
- Room uses destructive downgrade fallback; there is no migration beyond schema version 1.
- `AlarmReceiver` returns before rescheduling when notifications are denied.
- `CalendarScreen` remembers today only once; `AlarmScheduler` trusts stale persisted next-trigger timestamps.
- ICS byte accounting is character-based and import ignores UTC/TZID semantics.
- APK download accepts any successful 2xx response and optional content length.
- Holiday selected color is `#B91C1C`, while the repository contract requires `#EF4444`.
- Current Compose BOM/Material 3 pins are old relative to current stable AndroidX releases.
- Existing tests are core Jalali unit tests only; no `androidTest` sources were present.
- The app previously requested `REQUEST_INSTALL_PACKAGES` and downloaded/launched self-update APKs from GitHub. Google Play policy explicitly disallows this permission for self-updates and requires Play-distributed apps to update through Google Play; this is a concrete harmful/policy-risk trigger.
- Release signing files exist under the ignored `signing/` directory, while no signing material is tracked in Git. The app build refuses to create a release build without that keystore.
- Battery settings previously opened the direct Doze-exemption action; this was a second Play policy risk because the app can use exact alarms without asking to bypass all battery optimization.
- The checked-in version metadata was build 22; the remediation build is 23 to preserve update ordering for distribution.
- The first latest-BOM attempt was rejected by the build's AAR metadata check because Compose 1.12.1 requires compileSdk 37 and AGP 9.1. The compatible final selection is BOM 2025.08.00 plus Material 3 1.4.0 on compileSdk 35/AGP 8.7.3.
- Lint's Compose/Lifecycle Kotlin UAST detectors crashed with `IncompatibleClassChangeError` before reporting source issues. The final lint configuration disables only the detector IDs named by lint (`RememberInComposition`, `FrequentlyChangingValue`, `NullSafeMutableLiveData`, `AutoboxingStateCreation`) across Android modules; the lint gate then passed.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Use one canonical Jalali engine and make recurrence semantics explicit | Prevent boundary drift and make leap-day/month-end behavior testable |
| Treat notification delivery failure as non-fatal to recurrence advancement | A denied notification must not erase future occurrences |
| Use explicit Room migrations and safe converters | Reminder data is user data and must survive upgrades/downgrades |
| Remove custom APK self-update instead of requesting an install permission | A calendar is not an eligible core package-installer app; use the official distribution channel and a browser release page |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Android backup exclusions protect private reminder and preference data; enabling cloud export requires explicit destination authorization | No mutation made; keep exclusions until the backup destination and privacy policy are explicitly approved |
| Google harmful-app risk is not fully classifiable from source alone | Removed the concrete self-update trigger; Play Console/Play Protect classification still requires a signed release upload or appeal if an external warning persists |
| Direct battery exemption is not required for the alarm contract | Open the standard optimization list and let the user make the system-level choice |

## Resources
-

## Rollover and independent widget implementation (2026-11-15)
- Full widget design and debugging were approved; the owner explicitly waived physical-device execution and requested code/local checks only. Test seams are notification/alarm/system-intent outputs, widget settings/rendering/navigation and canonical recurrence projection.
- Displayed solar dates use Asia/Tehran, but the previous midnight scheduler used the device timezone. The previous receiver read reminders before updating the date and only armed the next alarm at the end of the chain. Startup did not publish/recover the date without MainActivity. Corrected these independent source failure paths; OEM-specific attribution still requires hardware and is not claimed.
- Date publication and the next alarm now precede reminder I/O. Application startup and screen/unlock/date/time signals recover a missing or stale notification. Database failure retains current date with an explicit pending summary rather than presenting fake zero reminders. Receiver work stays owned by goAsync and finishes in finally.
- Release resource-shrinker evidence in the existing 1.3.5 resources.txt marks all ic_stat_day_* icons retained. Therefore removed status resources are not an established cause; no speculative resource-keep patch was added.
- Five widget types share one snapshot/update path, independent persisted configuration, semantic palettes, size-aware rendering, guarded month actions, exact date/reminder intents and draft-replacement confirmation. Agenda Binder output is bounded to 200 rows with an explicit full-calendar row for overflow; legacy Android uses the collection service. CI is strengthened to include the pure calendar tests and all four mandatory gates.

## Persian Calendar reference review (2026-11-14)
- User approved independent implementation, our visual identity, transparent inspiration credit and preserving MIT. This does not approve copying GPL code, XML, icons, fonts, datasets or previews.
- Reference snapshot: `0011cf09f495138459e107f373f16ea414e2176b`, read-only clone in `/tmp/soh3il-persian-calendar-reference`; SOH3IL baseline `69aa31c3fcbebfcfbd90fc6fa833be4738adc552`.
- Reference has 12 concrete provider classes registered in its manifest. It supports per-widget text scale, configuration previews, global widget colors/content options, size-aware rendering, month navigation, week and agenda variants. Preview images were visually inspected locally.
- Current SOH3IL has three providers, fixed dark widget resource colors, root-only month click, no provider-specific size-change override and no per-instance widget settings. Sources were read directly because the graph index is dated 2026-11-09 and flags changed/new files.
- Newly observed source defects: daily summary falls back to the first past reminder but labels it next; requestUpdate and auxiliary database-loading catches are empty. MainActivity already has reminders but does not pass them to updateAllWidgets, allowing redundant per-widget database reads. These findings are not fixed in the reference-review phase.
- Reference architectural ideas worth adapting: shared rendering for configuration preview, size-aware content density, one update coordinator, bounded event aggregation, focused lint rules and provider-failure tests. Do not copy its 1949-line UpdateUtils, global mutable state or replace exact reminder scheduling with WorkManager. No comparative runtime benchmark was run.
- Delivery artifacts: `docs/research/2026-11-14-persian-calendar-comparison.md` and `docs/superpowers/specs/2026-11-14-independent-widgets-design.md`.

## Approved v1.3.5 implementation design (2026-11-14)
- Authorization: the user requested fixing the reviewed defects and releasing a new version. Work is single-assistant only, in the existing clean checkout on codex/widget-reliability-v135. No additional design or publication approval is required for that scope.
- Preserve the locked calendar gestures, header buttons, branding, holiday selection and Kotlin module boundaries. Keep the Room table schema unchanged; extend recurrence enum values by name so old records retain their meaning.
- Use explicit equal-width/equal-height LinearLayout rows for the RemoteViews month grid, filter official holidays, give providers distinct Persian labels and previews, and add a settings callback to an app-owned widget pin chooser. Verify real measured views on Android.
- Extract pure birthday date projection into core:jalali, accepting standard Gregorian full/yearless dates and slash separators with normalized digits. Search both Gregorian years intersecting the requested solar year; do not infer a Persian calendar solely from a numeric year range.
- Preserve small UI values with rememberSaveable and explicit savers for JalaliDate/PreReminder. Persist the selected editing reminder ID in the host and reload its record rather than serializing user data objects into Activity state.
- Return provider access failures as Result at the integration boundary, rethrow coroutine cancellation, and show a Persian failure message at the existing UI caller; do not report an empty successful sync on denial.
- Enforce the ICS byte cap in an InputStream wrapper before BufferedReader can materialize a line, raising an explicit size error. Separate Gregorian monthly/yearly recurrence from Jalali recurrence, retaining intervals and anchor dates. RFC 5545 requires nonexistent dates to be skipped for standard rules; existing Jalali SNAP_TO_LAST_DAY behavior remains unchanged.
- Verification order: existing core baseline; failing regression tests; smallest implementation; four required Gradle gates; device layout/state checks; signed release build; final review; version 1.3.5/build 26 and existing GitHub tag workflow. Do not weaken existing assertions or lint checks.
- GitHub CLI's inherited token returned 401; removing GH_TOKEN/GITHUB_TOKEN only for CLI subprocesses successfully used the existing saved account. No credentials are printed or changed.
