package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.nplusonequerycompanion.model.AssociationAccess
import dev.gaphunter.nplusonequerycompanion.model.LoopAssociationHit

/**
 * Finds Java `for (Entity e : entities)` loops whose body accesses a
 * lazy JPA association directly on the raw loop variable at least once --
 * the classic N+1 shape (fetch a collection, then trigger one extra query
 * per element while iterating it).
 *
 * **v0.1 scope, stated honestly (same pattern as every other plugin's
 * documented limitations):** only the raw loop variable is checked, not a
 * variable derived from it (`var order = e; order.getLines()` isn't
 * matched) -- avoids a full data-flow pass for a first version. Only
 * `PsiForeachStatement` is covered, not classic indexed `for`/streams
 * (`.forEach(...)`) -- v0.2 candidate, not built here.
 */
object JavaLoopAssociationFinder {

    fun findAll(file: PsiFile): List<LoopAssociationHit> {
        val hits = mutableListOf<LoopAssociationHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is PsiForeachStatement) {
                    hitFor(element)?.let { hits += it }
                }
            }
        })
        return hits
    }

    private fun hitFor(loop: PsiForeachStatement): LoopAssociationHit? {
        val parameter = loop.iterationParameter
        val body = loop.body ?: return null
        val loopKeyword = loop.firstChild ?: return null

        val accesses = mutableListOf<AssociationAccess>()
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element !is PsiMethodCallExpression) return

                val qualifier = element.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return
                if (qualifier.resolve() != parameter) return

                val method = element.resolveMethod() ?: return
                val match = JavaAssociationAnnotations.resolveFromMethod(method) ?: return
                if (!match.isLazyRisk) return

                accesses += AssociationAccess(match.kind, match.displayName)
            }
        })

        if (accesses.isEmpty()) return null
        return LoopAssociationHit(loopKeyword, accesses.distinct())
    }
}
