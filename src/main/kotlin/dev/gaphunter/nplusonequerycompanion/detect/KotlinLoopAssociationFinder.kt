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
 * is resolved to its actual declaration and compared by PSI identity
 * against the loop's own parameter, same as the Java side's
 * `qualifier.resolve() != parameter` check -- matching by simple name
 * text alone was tried first and found to double-count a real N+1 site
 * whenever an inner scope shadows the loop variable's name (a nested
 * `for` reusing the same short name, common in idiomatic Kotlin).
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
        val loopParameter = loop.loopParameter ?: return null
        val body = loop.body ?: return null
        val forKeyword = loop.forKeyword ?: return null

        val accesses = mutableListOf<AssociationAccess>()
        body.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                val receiverRef = expression.receiverExpression as? KtNameReferenceExpression ?: return
                val receiverTarget = receiverRef.references.firstNotNullOfOrNull { it.resolve() }
                if (receiverTarget != loopParameter) return

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
