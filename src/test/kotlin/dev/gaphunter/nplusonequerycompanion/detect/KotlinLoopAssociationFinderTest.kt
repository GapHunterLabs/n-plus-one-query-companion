package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinLoopAssociationFinderTest : BasePlatformTestCase() {

    private fun addCustomerEntity(associationAnnotation: String) {
        myFixture.addFileToProject(
            "Customer.kt",
            """
            import javax.persistence.*

            @Entity
            class Customer {
                $associationAnnotation
                var orders: List<Order> = emptyList()
            }
            """.trimIndent(),
        )
    }

    fun `test OneToMany with default fetch is flagged inside a for loop`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(customers: List<Customer>) {
                    for (c in customers) {
                        c.orders
                    }
                }
            }
            """.trimIndent(),
        )
        val hits = KotlinLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("orders", hits[0].accesses[0].displayName)
    }

    fun `test OneToMany with explicit EAGER fetch is not flagged`() {
        addCustomerEntity("@OneToMany(fetch = FetchType.EAGER)")
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(customers: List<Customer>) {
                    for (c in customers) {
                        c.orders
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test ManyToOne with default fetch (eager) is not flagged`() {
        myFixture.addFileToProject(
            "Order.kt",
            """
            import javax.persistence.*

            @Entity
            class Order {
                @ManyToOne
                var customer: Customer? = null
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(orders: List<Order>) {
                    for (o in orders) {
                        o.customer
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test ManyToOne with explicit LAZY fetch is flagged`() {
        myFixture.addFileToProject(
            "Order.kt",
            """
            import javax.persistence.*

            @Entity
            class Order {
                @ManyToOne(fetch = FetchType.LAZY)
                var customer: Customer? = null
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(orders: List<Order>) {
                    for (o in orders) {
                        o.customer
                    }
                }
            }
            """.trimIndent(),
        )
        val hits = KotlinLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("customer", hits[0].accesses[0].displayName)
    }

    fun `test unrelated property access inside the loop is not flagged`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(customers: List<Customer>) {
                    for (c in customers) {
                        println(c)
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test access on a derived variable, not the raw loop variable, is not flagged -- v0-1 documented scope limit`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(customers: List<Customer>) {
                    for (c in customers) {
                        val copy = c
                        copy.orders
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test a loop with no association access produces no hits and no crash`() {
        val file = myFixture.configureByText(
            "Report.kt",
            """
            class Report {
                fun run(names: List<String>) {
                    for (n in names) {
                        println(n.length)
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinLoopAssociationFinder.findAll(file).isEmpty())
    }
}
