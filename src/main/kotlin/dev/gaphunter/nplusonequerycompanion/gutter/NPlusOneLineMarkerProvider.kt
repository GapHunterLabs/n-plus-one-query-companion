package dev.gaphunter.nplusonequerycompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.nplusonequerycompanion.detect.JavaLoopAssociationFinder
import dev.gaphunter.nplusonequerycompanion.detect.KotlinLoopAssociationFinder
import dev.gaphunter.nplusonequerycompanion.model.LoopAssociationHit
import dev.gaphunter.nplusonequerycompanion.review.ReviewPrompt

/**
 * Gutter icon on the `for`/`for (x in y)` keyword of any loop whose body
 * accesses a lazy JPA association (`@OneToMany`/`@ManyToMany` by default,
 * or `@ManyToOne`/`@OneToOne` with an explicit `fetch = FetchType.LAZY`)
 * directly on the raw loop variable -- the classic N+1 query shape.
 *
 * **Leaf-anchored from the start**: both finders
 * already hand back the real keyword leaf token
 * ([LoopAssociationHit.loopKeyword] -- Java's `PsiForeachStatement.firstChild`,
 * Kotlin's `KtForExpression.forKeyword`), so no extra leaf-descent is
 * needed here, unlike providers that anchor on a composite literal node.
 */
class NPlusOneLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "N+1 query risk"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaLoopAssociationFinder.findAll(file)
            "kotlin" -> KotlinLoopAssociationFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByKeyword = hits.associateBy { it.loopKeyword }
        for (element in elements) {
            val hit = hitsByKeyword[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: LoopAssociationHit): LineMarkerInfo<PsiElement> {
        val tooltip = tooltipFor(hit)
        return LineMarkerInfo(
            hit.loopKeyword,
            hit.loopKeyword.textRange,
            NPlusOneIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }

    private fun tooltipFor(hit: LoopAssociationHit): String {
        val names = hit.accesses.joinToString(", ") { "${it.displayName} (${it.kind.label})" }
        return "Possible N+1 query: this loop accesses a lazy association per iteration -- $names"
    }
}
