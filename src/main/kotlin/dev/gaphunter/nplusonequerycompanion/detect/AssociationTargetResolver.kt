package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import dev.gaphunter.nplusonequerycompanion.model.AssociationAnnotationMatch
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Single entry point both language finders use once they've resolved an
 * association-access reference (a getter call, a property reference) to
 * its declaration. A Kotlin property backing a JPA entity can resolve to
 * a raw [KtProperty]/[KtParameter] (same-module Kotlin declaration) or to
 * a [PsiMethod]/[PsiField] (Java interop, or a Kotlin light-class view) --
 * this dispatches to the right resolver either way instead of each finder
 * needing to know the difference.
 */
object AssociationTargetResolver {

    fun resolve(target: PsiElement): AssociationAnnotationMatch? = when (target) {
        is PsiMethod -> JavaAssociationAnnotations.resolveFromMethod(target)
        is PsiField -> JavaAssociationAnnotations.resolveFromField(target)
        is KtProperty -> KotlinAssociationAnnotations.resolveFromProperty(target)
        is KtParameter -> KotlinAssociationAnnotations.resolveFromParameter(target)
        else -> null
    }
}
