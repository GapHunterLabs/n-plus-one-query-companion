package dev.gaphunter.nplusonequerycompanion.model

/**
 * A confirmed JPA association annotation found on a field/property/getter,
 * already reduced to language-neutral data -- [fetchAttributeText] is the
 * raw text of the annotation's `fetch = ...` attribute if present (e.g.
 * `"FetchType.EAGER"`), or `null` if the annotation didn't declare one
 * explicitly. Kept as plain text (not a resolved enum constant) because
 * neither the Java nor the Kotlin resolvers depend on the JPA jar being on
 * the classpath -- same reasoning as [AssociationKind]'s simple-name match.
 */
data class AssociationAnnotationMatch(
    val kind: AssociationKind,
    val displayName: String,
    val fetchAttributeText: String?,
) {
    /**
     * True when this association is lazy-loaded (the real N+1 risk case):
     * an explicit `fetch = FetchType.LAZY` always wins, an explicit
     * `fetch = FetchType.EAGER` always clears the risk, and with no
     * explicit attribute the JPA spec default per [AssociationKind] applies.
     */
    val isLazyRisk: Boolean
        get() {
            val text = fetchAttributeText ?: return kind.defaultIsLazy
            return when {
                text.contains("EAGER") -> false
                text.contains("LAZY") -> true
                else -> kind.defaultIsLazy
            }
        }
}

/** One risky association access found inside a loop body. */
data class AssociationAccess(val kind: AssociationKind, val displayName: String)

/** A loop whose body accesses 1+ lazy association per iteration -- the whole N+1 finding. */
data class LoopAssociationHit(
    val loopKeyword: com.intellij.psi.PsiElement,
    val accesses: List<AssociationAccess>,
)
