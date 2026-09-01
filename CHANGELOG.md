<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# N+1 Query Companion Changelog

## [Unreleased]

## [0.2.0]

### Added

- Java: follows exactly ONE method-call hop into a same-class helper
  method the loop body calls (no qualifier, or explicit `this.`),
  passing the loop's iteration variable as an argument -- if the
  helper's own corresponding parameter accesses a lazy association,
  that's flagged too, not just direct inline access. Never follows a
  chain of two or more hops.

## [0.1.1]

### Added

- Review/star CTA: after 10 distinct real findings, a one-time
  notification asks whether to rate the plugin on Marketplace, with a
  permanent "Don't ask again" option. Standard mechanism used
  catalog-wide since 2026-08-24, rolled out
  to this plugin now.

## [0.1.0]

### Added

- Gutter warning icon on any Java/Kotlin `for`/`for (x in y)` loop whose
  body accesses a lazy JPA association (`@OneToMany`/`@ManyToMany` by
  default, or `@ManyToOne`/`@OneToOne` with an explicit
  `fetch = FetchType.LAZY`) directly on the raw loop variable.
- Tooltip names the exact association field/property and annotation
  responsible for the N+1 risk.
- 100% static PSI analysis, Java and Kotlin, no network calls, no
  telemetry. Free.

[Unreleased]: https://github.com/GapHunterLabs/n-plus-one-query-companion/compare/0.2.0...HEAD
[0.2.0]: https://github.com/GapHunterLabs/n-plus-one-query-companion/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/GapHunterLabs/n-plus-one-query-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/n-plus-one-query-companion/commits/0.1.0
