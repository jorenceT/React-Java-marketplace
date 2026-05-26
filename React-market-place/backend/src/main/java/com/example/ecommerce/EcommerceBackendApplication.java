package com.example.ecommerce;

public class EcommerceBackendApplication {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "10000"));
        new MarketplaceServer(port).start();
    }
}
