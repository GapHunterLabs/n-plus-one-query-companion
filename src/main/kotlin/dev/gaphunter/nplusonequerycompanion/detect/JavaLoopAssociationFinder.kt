package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.nplusonequerycompanion.model.AssociationAccess
import dev.gaphunter.nplusonequerycompanion.model.LoopAssociationHit

/**
 * Finds Java `for (Entity e : entities)` loops whose body accesses a
 * lazy JPA association directly on the raw loop variable at least once --
 * the classic N+1 shape (fetch a collection, then trigger one extra query
 * per element while iterating it).
 *
 * **v0.2 extends this one method-call hop deeper**: if the loop body
 * calls a same-class helper method (no qualifier, or an explicit
 * `this.` qualifier), passing the loop's own iteration variable as an
 * argument, and that helper's OWN corresponding parameter is itself
 * used to access a lazy association -- that's exactly as real an N+1
 * site as accessing the association inline, just factored into a
 * helper. `JOptimize` (Marketplace, confirmed competitor) covers N+1
 * only via direct JPA/Hibernate annotation access, the same depth as
 * this plugin's own v0.1 -- this one-hop angle is a deeper granularity
 * not confirmed in its coverage.
 *
 * **v0.1 scope, stated honestly (same pattern as every other plugin's
 * documented limitations):** only the raw loop variable is checked, not a
 * variable derived from it (`var order = e; order.getLines()` isn't
 * matched) -- avoids a full data-flow pass for a first version. Only
 * `PsiForeachStatement` is covered, not classic indexed `for`/streams
 * (`.forEach(...)`).
 *
 * **v0.2 scope, stated honestly:** exactly ONE method-call hop, bounded
 * to a same-class helper (no qualifier/`this.` only) -- never follows a
 * chain of two or more hops, and never a call whose qualifier is some
 * other object.
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

                val qualifierExpr = element.methodExpression.qualifierExpression
                if (qualifierExpr == null || qualifierExpr.text == "this") {
                    // No qualifier (or explicit `this.`) -- a same-class
                    // helper call, v0.2's one-hop extension.
                    accesses += helperMethodAccesses(element, parameter)
                    return
                }

                val qualifier = qualifierExpr as? PsiReferenceExpression ?: return
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

    /**
     * v0.2: follows exactly one call into a same-class helper method
     * that the loop body calls, when the loop's own iteration variable
     * is passed as one of its arguments -- if the helper's
     * CORRESPONDING parameter is itself used to access a lazy
     * association, that's the same real N+1 site, one method away.
     * Never recurses further into whatever the helper itself calls.
     */
    private fun helperMethodAccesses(call: PsiMethodCallExpression, iterationParameter: PsiParameter): List<AssociationAccess> {
        val callee = call.resolveMethod() ?: return emptyList()
        val calleeBody = callee.body ?: return emptyList()

        val args = call.argumentList.expressions
        val paramIndex = args.indexOfFirst { (it as? PsiReferenceExpression)?.resolve() == iterationParameter }
        if (paramIndex < 0) return emptyList()
        val calleeParam = callee.parameterList.parameters.getOrNull(paramIndex) ?: return emptyList()

        val accesses = mutableListOf<AssociationAccess>()
        calleeBody.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(innerCall: PsiMethodCallExpression) {
                super.visitMethodCallExpression(innerCall)
                val qualifier = innerCall.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return
                if (qualifier.resolve() != calleeParam) return

                val method = innerCall.resolveMethod() ?: return
                val match = JavaAssociationAnnotations.resolveFromMethod(method) ?: return
                if (!match.isLazyRisk) return

                accesses += AssociationAccess(match.kind, match.displayName)
            }
        })
        return accesses
    }
}
