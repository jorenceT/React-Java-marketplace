package com.example.ecommerce.service;

import com.example.ecommerce.domain.Product;
import com.example.ecommerce.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class CatalogService {
    private final ConcurrentHashMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public CatalogService() {
        seed();
    }

    public List<ProductResponse> getProducts() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::getId))
                .map(ProductResponse::new)
                .collect(Collectors.toList());
    }

    public ProductResponse getProduct(Long id) {
        return new ProductResponse(requireProduct(id));
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product requireProduct(Long id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public void reserveStock(Long productId, int quantity) {
        products.compute(productId, (id, product) -> {
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Not enough stock for " + product.getName());
            }
            return product.withStock(product.getStock() - quantity);
        });
    }

    private void seed() {
        add("Nova Headphones", "Wireless headphones with rich sound and all-day comfort.", "Audio", new BigDecimal("149.00"), "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80", 4.8, 12);
        add("Atlas Watch", "Minimal stainless-steel watch with a clean silhouette.", "Accessories", new BigDecimal("219.00"), "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?auto=format&fit=crop&w=900&q=80", 4.6, 8);
        add("Cloud Runner Sneakers", "Lightweight everyday sneakers built for comfort.", "Footwear", new BigDecimal("129.00"), "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80", 4.9, 15);
        add("Studio Lamp", "Warm ambient lamp for modern desks and bedside setups.", "Home", new BigDecimal("89.00"), "https://images.unsplash.com/photo-1517705008128-361805f42e86?auto=format&fit=crop&w=900&q=80", 4.7, 10);
        add("Everyday Backpack", "Durable backpack with laptop sleeve and hidden pocket.", "Bags", new BigDecimal("99.00"), "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=80", 4.5, 20);
        add("Sage Bottle", "Insulated bottle that keeps drinks cold or hot for hours.", "Lifestyle", new BigDecimal("39.00"), "https://images.unsplash.com/photo-1526401485004-2aa6a9d7f2d1?auto=format&fit=crop&w=900&q=80", 4.4, 30);
    }

    private void add(String name, String description, String category, BigDecimal price, String imageUrl, double rating, int stock) {
        long id = nextId.getAndIncrement();
        products.put(id, new Product(id, name, description, category, price, imageUrl, rating, stock));
    }

    public List<String> categories() {
        return new ArrayList<>(products.values().stream()
                .map(Product::getCategory)
                .distinct()
                .sorted()
                .toList());
    }
}
