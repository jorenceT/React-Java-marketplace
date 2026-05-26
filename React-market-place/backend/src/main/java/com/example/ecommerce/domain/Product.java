package com.example.ecommerce.domain;

import java.math.BigDecimal;

public class Product {
    private final Long id;
    private final String name;
    private final String description;
    private final String category;
    private final BigDecimal price;
    private final String imageUrl;
    private final double rating;
    private final int stock;

    public Product(Long id, String name, String description, String category, BigDecimal price, String imageUrl, double rating, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getRating() {
        return rating;
    }

    public int getStock() {
        return stock;
    }

    public Product withStock(int updatedStock) {
        return new Product(id, name, description, category, price, imageUrl, rating, updatedStock);
    }
}

