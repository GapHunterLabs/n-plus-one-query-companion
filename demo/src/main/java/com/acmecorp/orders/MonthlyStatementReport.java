package com.acmecorp.orders;

import java.math.BigDecimal;
import java.util.List;

/**
 * Realistic acmecorp report code — generates a monthly statement summary
 * per customer. This is the demo target for N+1 Query Companion: open
 * this file in the sandbox and the "for" keyword below should show a
 * warning icon (c.getOrders() is a lazy @OneToMany access per iteration).
 */
public class MonthlyStatementReport {

    public void printStatements(List<Customer> customers) {
        for (Customer c : customers) {
            BigDecimal total = BigDecimal.ZERO;
            for (Order o : c.getOrders()) {
                total = total.add(o.getTotalAmount());
            }
            System.out.println(c.getName() + ": " + total);
        }
    }
}
