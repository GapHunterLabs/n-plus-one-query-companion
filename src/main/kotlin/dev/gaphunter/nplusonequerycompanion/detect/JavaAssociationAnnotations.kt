package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import dev.gaphunter.nplusonequerycompanion.model.AssociationAnnotationMatch
import dev.gaphunter.nplusonequerycompanion.model.AssociationKind

/**
 * Resolves a Java field/getter to its JPA association annotation, if any.
 * Handles both real JPA styles: annotation directly on the field
 * (field-access strategy) and annotation on the getter (property-access
 * strategy) -- checks the accessor itself first, then falls back to the
 * matching field by name.
 */
object JavaAssociationAnnotations {

    fun resolveFromMethod(method: PsiMethod): AssociationAnnotationMatch? {
        findOn(method, method.name)?.let { return it }
        val propertyName = propertyNameFromAccessor(method.name) ?: return null
        val field = method.containingClass?.findFieldByName(propertyName, true) ?: return null
        return findOn(field, field.name)
    }

    fun resolveFromField(field: PsiField): AssociationAnnotationMatch? = findOn(field, field.name)

    private fun findOn(owner: PsiModifierListOwner, displayName: String): AssociationAnnotationMatch? {
        val annotations = owner.modifierList?.annotations ?: return null
        for (annotation in annotations) {
            val simpleName = annotation.nameReferenceElement?.referenceName ?: continue
            val kind = AssociationKind.bySimpleName(simpleName) ?: continue
            val fetchText = annotation.findAttributeValue("fetch")?.text
            return AssociationAnnotationMatch(kind, displayName, fetchText)
        }
        return null
    }

    /** `getOrders` -> `orders`, `isActive` -> `active`. Returns null for anything that isn't a plain JavaBean accessor name. */
    private fun propertyNameFromAccessor(methodName: String): String? {
        val stripped = when {
            methodName.length > 3 && methodName.startsWith("get") -> methodName.removePrefix("get")
            methodName.length > 2 && methodName.startsWith("is") -> methodName.removePrefix("is")
            else -> return null
        }
        return stripped.replaceFirstChar { it.lowercaseChar() }
    }
}
