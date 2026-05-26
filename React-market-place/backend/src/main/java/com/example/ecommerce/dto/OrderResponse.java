package com.example.ecommerce.dto;

import com.example.ecommerce.domain.Order;
import com.example.ecommerce.domain.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {
    private final Long id;
    private final String customerName;
    private final String email;
    private final String address;
    private final String status;
    private final BigDecimal total;
    private final Instant createdAt;
    private final List<OrderItem> items;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.customerName = order.getCustomerName();
        this.email = order.getEmail();
        this.address = order.getAddress();
        this.status = order.getStatus();
        this.total = order.getTotal();
        this.createdAt = order.getCreatedAt();
        this.items = order.getItems();
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

