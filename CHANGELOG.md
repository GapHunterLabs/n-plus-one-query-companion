<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# N+1 Query Companion Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/GapHunterLabs/n-plus-one-query-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/n-plus-one-query-companion/commits/0.1.0
