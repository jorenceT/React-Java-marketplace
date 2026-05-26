package com.example.ecommerce.service;

import com.example.ecommerce.domain.Order;
import com.example.ecommerce.domain.OrderItem;
import com.example.ecommerce.domain.Product;
import com.example.ecommerce.dto.OrderItemRequest;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.dto.OrderResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {
    private final CatalogService catalogService;
    private final AtomicLong nextId = new AtomicLong(1001);
    private final ConcurrentHashMap<Long, Order> orders = new ConcurrentHashMap<>();

    public OrderService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public List<OrderResponse> getOrders() {
        return orders.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(OrderResponse::new)
                .toList();
    }

    public OrderResponse createOrder(OrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one cart item is required");
        }

        synchronized (catalogService) {
            Map<Long, Integer> requestedQuantities = new HashMap<>();
            Map<Long, Product> productsById = new HashMap<>();

            for (OrderItemRequest itemRequest : request.getItems()) {
                if (itemRequest.getProductId() == null) {
                    throw new IllegalArgumentException("Each item needs a product id");
                }
                if (itemRequest.getQuantity() < 1) {
                    throw new IllegalArgumentException("Item quantity must be at least 1");
                }

                Product product = catalogService.requireProduct(itemRequest.getProductId());
                int updatedQuantity = requestedQuantities.getOrDefault(product.getId(), 0) + itemRequest.getQuantity();
                if (product.getStock() < updatedQuantity) {
                    throw new IllegalArgumentException("Not enough stock for " + product.getName());
                }

                requestedQuantities.put(product.getId(), updatedQuantity);
                productsById.putIfAbsent(product.getId(), product);
            }

            for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
                catalogService.reserveStock(entry.getKey(), entry.getValue());
            }

            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemRequest itemRequest : request.getItems()) {
                Product product = productsById.get(itemRequest.getProductId());
                BigDecimal quantity = BigDecimal.valueOf(itemRequest.getQuantity());
                BigDecimal lineTotal = product.getPrice().multiply(quantity);
                total = total.add(lineTotal);
                orderItems.add(new OrderItem(product.getId(), product.getName(), itemRequest.getQuantity(), product.getPrice(), lineTotal));
            }

            Long id = nextId.getAndIncrement();
            Order order = new Order(
                    id,
                    request.getCustomerName(),
                    request.getEmail(),
                    request.getAddress(),
                    "CONFIRMED",
                    total,
                    Instant.now(),
                    List.copyOf(orderItems)
            );

            orders.put(id, order);
            return new OrderResponse(order);
        }
    }
}
