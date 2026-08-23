package dev.gaphunter.nplusonequerycompanion.detect

import dev.gaphunter.nplusonequerycompanion.model.AssociationAnnotationMatch
import dev.gaphunter.nplusonequerycompanion.model.AssociationKind
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Resolves a Kotlin property/constructor-parameter declaration to its JPA
 * association annotation, if any. Reads [KtAnnotationEntry] directly (raw
 * PSI) rather than going through a light-class [com.intellij.psi.PsiAnnotation]
 * view -- keeps this K1/K2-neutral (structural PSI only, same discipline
 * documented in `feature-flag-reference-companion`'s `plugin.xml`).
 */
object KotlinAssociationAnnotations {

    fun resolveFromProperty(property: KtProperty): AssociationAnnotationMatch? {
        val name = property.name ?: return null
        return find(property.annotationEntries, name)
    }

    fun resolveFromParameter(parameter: KtParameter): AssociationAnnotationMatch? {
        val name = parameter.name ?: return null
        return find(parameter.annotationEntries, name)
    }

    private fun find(entries: List<KtAnnotationEntry>, displayName: String): AssociationAnnotationMatch? {
        for (entry in entries) {
            val simpleName = entry.shortName?.asString() ?: continue
            val kind = AssociationKind.bySimpleName(simpleName) ?: continue
            val fetchText = fetchArgumentText(entry)
            return AssociationAnnotationMatch(kind, displayName, fetchText)
        }
        return null
    }

    private fun fetchArgumentText(entry: KtAnnotationEntry): String? {
        val fetchArg = entry.valueArguments.firstOrNull { arg ->
            arg.getArgumentName()?.asName?.asString() == "fetch"
        }
        return fetchArg?.getArgumentExpression()?.text
    }
}
