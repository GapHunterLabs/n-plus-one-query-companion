package dev.gaphunter.nplusonequerycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssociationAnnotationMatchTest {

    @Test
    fun `OneToMany with no explicit fetch defaults to lazy risk`() {
        val match = AssociationAnnotationMatch(AssociationKind.ONE_TO_MANY, "orders", fetchAttributeText = null)
        assertTrue(match.isLazyRisk)
    }

    @Test
    fun `OneToMany with explicit EAGER clears the risk`() {
        val match = AssociationAnnotationMatch(AssociationKind.ONE_TO_MANY, "orders", fetchAttributeText = "FetchType.EAGER")
        assertFalse(match.isLazyRisk)
    }

    @Test
    fun `ManyToOne with no explicit fetch defaults to eager, no risk`() {
        val match = AssociationAnnotationMatch(AssociationKind.MANY_TO_ONE, "customer", fetchAttributeText = null)
        assertFalse(match.isLazyRisk)
    }

    @Test
    fun `ManyToOne with explicit LAZY is a risk`() {
        val match = AssociationAnnotationMatch(AssociationKind.MANY_TO_ONE, "customer", fetchAttributeText = "FetchType.LAZY")
        assertTrue(match.isLazyRisk)
    }

    @Test
    fun `unrecognized fetch text falls back to the annotation default`() {
        val match = AssociationAnnotationMatch(AssociationKind.MANY_TO_MANY, "tags", fetchAttributeText = "someWeirdExpression")
        assertTrue(match.isLazyRisk)
    }

    @Test
    fun `bySimpleName matches the 4 real JPA association annotations only`() {
        assertEquals(AssociationKind.ONE_TO_MANY, AssociationKind.bySimpleName("OneToMany"))
        assertEquals(AssociationKind.MANY_TO_MANY, AssociationKind.bySimpleName("ManyToMany"))
        assertEquals(AssociationKind.MANY_TO_ONE, AssociationKind.bySimpleName("ManyToOne"))
        assertEquals(AssociationKind.ONE_TO_ONE, AssociationKind.bySimpleName("OneToOne"))
        assertEquals(null, AssociationKind.bySimpleName("Entity"))
        assertEquals(null, AssociationKind.bySimpleName("Column"))
    }
}
