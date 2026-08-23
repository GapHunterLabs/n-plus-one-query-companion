# N+1 Query Companion

Gutter icon on any Java/Kotlin `for` loop whose body accesses a
lazy-loaded JPA association directly on the raw loop variable — the
classic N+1 query shape: fetch a collection, then trigger one extra query
per element while iterating it.

```java
for (Customer c : customers) {
    c.getOrders(); // ⚠ @OneToMany, lazy by default -- one query per customer
}
```

## Why it exists

Static N+1 detection already has a real, paid-adjacent competitor in this
exact niche: **JPA Fetch Lens** (JetBrains Marketplace, real installs).
Reading its published feature list and source confirms it is 100%
hover/visualization — it shows you where an association *might* fire an
extra query, but nothing in it marks the loop itself or gives you a
fix-oriented tooltip at the exact site the extra query happens. N+1 Query
Companion covers the same underlying JPA-annotation signal but anchors
the warning on the loop (not the field declaration), so the fix target is
obvious without cross-referencing two places in the file.

## Why built this way

- **100% static PSI analysis, no runtime/SQL involved.** Nothing here
  connects to a database, parses JPQL/HQL, or runs the application — it
  only reads annotations already in your source (`@OneToMany`,
  `@ManyToMany`, `@ManyToOne`, `@OneToOne`) and the JPA spec's own default
  fetch strategy per annotation (`LAZY` for the first two, `EAGER` for the
  last two) to decide risk.
- **Matches JPA annotations by simple name only** — works whether the
  real `jakarta.persistence`/`javax.persistence` jar is on the classpath
  or not, same contract as every other framework-annotation detector in
  this catalog (e.g. `feature-flag-reference-companion`'s method-name
  matching).
- **Java and Kotlin, one shared model.** The Kotlin side resolves
  references via the plain `PsiElement.references` API — never the
  Kotlin-specific `mainReference` extension or the Analysis
  API/`BindingContext` — the same K1/K2-neutral discipline already
  proven in `turbo-log-companion`'s `StatementFinder` and
  `api-security-companion`'s `KotlinTypeAnnotationResolver`.

## v0.1 scope — stated honestly, not exhaustively

- Only the **raw loop variable** is checked. `for (Customer c : customers)
  { Customer copy = c; copy.getOrders(); }` is not flagged — a real
  limitation (would need a small data-flow pass), not a bug.
- Only classic `for (Type x : collection)` / `for (x in collection)` loops
  are covered. `.forEach(...)`/stream chains are a v0.2 candidate.
- No attempt to detect that the N+1 is already fixed via an explicit
  `JOIN FETCH`/`@EntityGraph` in a query string elsewhere — v0.1 only
  reasons about the annotation and the loop shape, not about JPQL/HQL
  text. A loop that's actually safe because of a fetch join upstream can
  still show the warning; read the tooltip and confirm before "fixing"
  something already handled.

## Usage

Open any Java/Kotlin file with a `for`/`for (x in y)` loop over JPA
entities. If the loop body accesses a lazy association directly on the
loop variable, a warning icon appears on the loop keyword — hover it for
the exact association and annotation responsible.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
