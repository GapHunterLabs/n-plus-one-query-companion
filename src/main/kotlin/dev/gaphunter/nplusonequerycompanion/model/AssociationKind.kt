package dev.gaphunter.nplusonequerycompanion.model

/**
 * The 4 real JPA/Jakarta Persistence association annotations this plugin
 * recognizes, matched by simple name only (no dependency on the
 * `jakarta.persistence`/`javax.persistence` jar being on the classpath --
 * same "match by name, not resolved symbol" contract as
 * `feature-flag-reference-companion`'s `FlagMethodNames`).
 *
 * [defaultIsLazy] encodes the real JPA spec default fetch strategy per
 * annotation -- `@OneToMany`/`@ManyToMany` default to `LAZY`,
 * `@ManyToOne`/`@OneToOne` default to `EAGER`. This is what lets
 * [dev.gaphunter.nplusonequerycompanion.detect.AssociationAnnotationMatch]
 * decide risk without needing an explicit `fetch = ...` attribute present.
 */
enum class AssociationKind(val label: String, val defaultIsLazy: Boolean) {
    ONE_TO_MANY("@OneToMany", defaultIsLazy = true),
    MANY_TO_MANY("@ManyToMany", defaultIsLazy = true),
    MANY_TO_ONE("@ManyToOne", defaultIsLazy = false),
    ONE_TO_ONE("@OneToOne", defaultIsLazy = false);

    companion object {
        private val BY_SIMPLE_NAME: Map<String, AssociationKind> = mapOf(
            "OneToMany" to ONE_TO_MANY,
            "ManyToMany" to MANY_TO_MANY,
            "ManyToOne" to MANY_TO_ONE,
            "OneToOne" to ONE_TO_ONE,
        )

        fun bySimpleName(simpleName: String): AssociationKind? = BY_SIMPLE_NAME[simpleName]
    }
}
