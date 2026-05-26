package com.example.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class Order {
    private final Long id;
    private final String customerName;
    private final String email;
    private final String address;
    private final String status;
    private final BigDecimal total;
    private final Instant createdAt;
    private final List<OrderItem> items;

    public Order(Long id, String customerName, String email, String address, String status, BigDecimal total, Instant createdAt, List<OrderItem> items) {
        this.id = id;
        this.customerName = customerName;
        this.email = email;
        this.address = address;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}

