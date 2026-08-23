package com.acmecorp.orders;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Customer {

    private Long id;
    private String name;

    @OneToMany
    private List<Order> orders;

    // @ManyToOne(fetch = FetchType.LAZY) style demo lives in Order.java

    public List<Order> getOrders() {
        return orders;
    }

    public String getName() {
        return name;
    }
}
