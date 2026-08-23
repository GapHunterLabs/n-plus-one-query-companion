package dev.gaphunter.nplusonequerycompanion.detect

import dev.gaphunter.nplusonequerycompanion.model.AssociationAccess
import dev.gaphunter.nplusonequerycompanion.model.LoopAssociationHit
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import com.intellij.psi.PsiFile

/**
 * Finds Kotlin `for (order in orders)` loops whose body accesses a lazy
 * JPA association directly on the raw loop variable at least once. Same
 * v0.1 scope limits as [JavaLoopAssociationFinder] (raw loop variable
 * only, `for` loops only -- no `.forEach`/stream chains).
 *
 * **Resolution discipline:** uses `PsiElement.references.firstNotNullOfOrNull
 * { it.resolve() }` (plain platform API), never the Kotlin-specific
 * `mainReference` extension -- same PSI-only, no-Analysis-API contract
 * already proven safe under both K1 and K2 elsewhere in this catalog
 * (`turbo-log-companion`'s `StatementFinder.findKotlin`,
 * `api-security-companion`'s `KotlinTypeAnnotationResolver`). The receiver
 * itself is matched by simple name text, never resolved, keeping the
 * "raw loop variable only" v0.1 scope both intentional and cheap.
 */
object KotlinLoopAssociationFinder {

    fun findAll(file: PsiFile): List<LoopAssociationHit> {
        if (file !is KtFile) return emptyList()
        val hits = mutableListOf<LoopAssociationHit>()

        file.accept(object : KtTreeVisitorVoid() {
            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)
                hitFor(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(loop: KtForExpression): LoopAssociationHit? {
        val paramName = loop.loopParameter?.name ?: return null
        val body = loop.body ?: return null
        val forKeyword = loop.forKeyword ?: return null

        val accesses = mutableListOf<AssociationAccess>()
        body.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                val receiverName = (expression.receiverExpression as? KtNameReferenceExpression)?.getReferencedName()
                if (receiverName != paramName) return

                val selector = expression.selectorExpression
                val nameRef: KtNameReferenceExpression = when (selector) {
                    is KtCallExpression -> selector.calleeExpression as? KtNameReferenceExpression
                    is KtNameReferenceExpression -> selector
                    else -> null
                } ?: return

                val target = nameRef.references.firstNotNullOfOrNull { it.resolve() } ?: return
                val match = AssociationTargetResolver.resolve(target) ?: return
                if (!match.isLazyRisk) return

                accesses += AssociationAccess(match.kind, match.displayName)
            }
        })

        if (accesses.isEmpty()) return null
        return LoopAssociationHit(forKeyword, accesses.distinct())
    }
}
