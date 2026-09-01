package dev.gaphunter.nplusonequerycompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaLoopAssociationFinderTest : BasePlatformTestCase() {

    private fun addCustomerEntity(associationAnnotation: String) {
        myFixture.addFileToProject(
            "Customer.java",
            """
            import javax.persistence.*;
            import java.util.List;

            @Entity
            class Customer {
                $associationAnnotation
                private List<Order> orders;

                public List<Order> getOrders() { return orders; }
            }
            """.trimIndent(),
        )
    }

    fun `test OneToMany with default fetch is flagged inside a foreach loop`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        c.getOrders();
                    }
                }
            }
            """.trimIndent(),
        )
        val hits = JavaLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals(1, hits[0].accesses.size)
        assertEquals("orders", hits[0].accesses[0].displayName)
    }

    fun `test OneToMany with explicit EAGER fetch is not flagged`() {
        addCustomerEntity("@OneToMany(fetch = FetchType.EAGER)")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        c.getOrders();
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test ManyToOne with default fetch (eager) is not flagged`() {
        myFixture.addFileToProject(
            "Order.java",
            """
            import javax.persistence.*;

            @Entity
            class Order {
                @ManyToOne
                private Customer customer;

                public Customer getCustomer() { return customer; }
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Order> orders) {
                    for (Order o : orders) {
                        o.getCustomer();
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test ManyToOne with explicit LAZY fetch is flagged`() {
        myFixture.addFileToProject(
            "Order.java",
            """
            import javax.persistence.*;

            @Entity
            class Order {
                @ManyToOne(fetch = FetchType.LAZY)
                private Customer customer;

                public Customer getCustomer() { return customer; }
            }
            """.trimIndent(),
        )
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Order> orders) {
                    for (Order o : orders) {
                        o.getCustomer();
                    }
                }
            }
            """.trimIndent(),
        )
        val hits = JavaLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("customer", hits[0].accesses[0].displayName)
    }

    fun `test unrelated method calls inside the loop are not flagged`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        System.out.println(c);
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test access on a derived variable, not the raw loop variable, is not flagged -- v0-1 documented scope limit`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        Customer copy = c;
                        copy.getOrders();
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test v0-2 -- association access inside a same-class helper method, one hop, is flagged`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        process(c);
                    }
                }

                void process(Customer customer) {
                    customer.getOrders();
                }
            }
            """.trimIndent(),
        )
        val hits = JavaLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
        assertEquals("orders", hits[0].accesses[0].displayName)
    }

    fun `test v0-2 -- explicit this qualifier on the helper call is also followed`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        this.process(c);
                    }
                }

                void process(Customer customer) {
                    customer.getOrders();
                }
            }
            """.trimIndent(),
        )
        val hits = JavaLoopAssociationFinder.findAll(file)
        assertEquals(1, hits.size)
    }

    fun `test v0-2 -- a helper that does not access any association is not flagged`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        process(c);
                    }
                }

                void process(Customer customer) {
                    System.out.println(customer);
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test v0-2 -- a helper call not passing the loop variable is not flagged`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        logStart();
                    }
                }

                void logStart() {
                    System.out.println("start");
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test v0-2 -- a two-hop chain is never followed, documented scope limit`() {
        addCustomerEntity("@OneToMany")
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<Customer> customers) {
                    for (Customer c : customers) {
                        outer(c);
                    }
                }

                void outer(Customer customer) {
                    inner(customer);
                }

                void inner(Customer customer) {
                    customer.getOrders();
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }

    fun `test a loop with no association access produces no hits and no crash`() {
        val file = myFixture.configureByText(
            "Report.java",
            """
            import java.util.List;
            class Report {
                void run(List<String> names) {
                    for (String n : names) {
                        System.out.println(n.length());
                    }
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaLoopAssociationFinder.findAll(file).isEmpty())
    }
}
