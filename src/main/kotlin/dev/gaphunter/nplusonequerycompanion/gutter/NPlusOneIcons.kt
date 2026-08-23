package dev.gaphunter.nplusonequerycompanion.gutter

import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Reuses the platform's own bundled warning icon rather than a custom
 * `Icon` implementation -- same precedent as
 * `feature-flag-reference-companion`'s `FlagReferenceIcons` (icon member
 * already confirmed in real use elsewhere in this catalog).
 */
object NPlusOneIcons {
    /** A loop whose body accesses 1+ lazy JPA association per iteration. */
    val RISK: Icon = AllIcons.General.InspectionsWarningEmpty
}
