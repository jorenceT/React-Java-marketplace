package com.example.ecommerce.dto;

import com.example.ecommerce.domain.Product;
import java.math.BigDecimal;

public class ProductResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String category;
    private final BigDecimal price;
    private final String imageUrl;
    private final double rating;
    private final int stock;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.category = product.getCategory();
        this.price = product.getPrice();
        this.imageUrl = product.getImageUrl();
        this.rating = product.getRating();
        this.stock = product.getStock();
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
}

