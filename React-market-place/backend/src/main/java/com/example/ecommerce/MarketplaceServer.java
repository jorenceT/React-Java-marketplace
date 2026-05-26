package com.example.ecommerce;

import com.example.ecommerce.domain.OrderItem;
import com.example.ecommerce.dto.OrderItemRequest;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.dto.ProductResponse;
import com.example.ecommerce.service.CatalogService;
import com.example.ecommerce.service.OrderService;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarketplaceServer {
    private final int port;
    private final CatalogService catalogService = new CatalogService();
    private final OrderService orderService = new OrderService(catalogService);

    public MarketplaceServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Marketplace API running on http://localhost:" + port);
    }

    private final class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());

            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendNoContent(exchange);
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                if (path.equals("/api/health") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, Map.of("status", "ok", "timestamp", Instant.now().toString()));
                    return;
                }

                if (path.equals("/api/products") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, catalogService.getProducts());
                    return;
                }

                if (path.startsWith("/api/products/") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    long productId = parseId(path, "/api/products/");
                    sendJson(exchange, 200, catalogService.getProduct(productId));
                    return;
                }

                if (path.equals("/api/categories") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, catalogService.categories());
                    return;
                }

                if (path.equals("/api/orders") && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 200, orderService.getOrders());
                    return;
                }

                if (path.equals("/api/orders") && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String body = Json.readBody(exchange.getRequestBody());
                    OrderRequest request = toOrderRequest(Json.asObject(Json.parse(body)));
                    OrderResponse order = orderService.createOrder(request);
                    sendJson(exchange, 201, order);
                    return;
                }

                sendJson(exchange, 404, Map.of(
                        "error", "Not Found",
                        "message", "No route matches " + exchange.getRequestMethod() + " " + path
                ));
            } catch (IllegalArgumentException exception) {
                sendJson(exchange, 400, Map.of(
                        "error", "Bad Request",
                        "message", exception.getMessage()
                ));
            } catch (Exception exception) {
                sendJson(exchange, 500, Map.of(
                        "error", "Internal Server Error",
                        "message", exception.getMessage() == null ? "Unexpected server error" : exception.getMessage()
                ));
            }
        }

        private OrderRequest toOrderRequest(Map<String, Object> payload) {
            OrderRequest request = new OrderRequest();
            request.setCustomerName(asString(payload.get("customerName")));
            request.setEmail(asString(payload.get("email")));
            request.setAddress(asString(payload.get("address")));

            Object itemsValue = payload.get("items");
            if (itemsValue == null) {
                throw new IllegalArgumentException("Items are required");
            }

            List<Object> items = Json.asArray(itemsValue);
            List<OrderItemRequest> orderItems = new java.util.ArrayList<>();
            for (Object itemValue : items) {
                Map<String, Object> item = Json.asObject(itemValue);
                OrderItemRequest orderItem = new OrderItemRequest();
                orderItem.setProductId(asLong(item.get("productId")));
                orderItem.setQuantity(asInt(item.get("quantity")));
                orderItems.add(orderItem);
            }

            request.setItems(orderItems);
            return request;
        }

        private String asString(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof String string) {
                return string;
            }
            throw new IllegalArgumentException("Expected a string value");
        }

        private Long asLong(Object value) {
            if (value instanceof BigDecimal decimal) {
                return decimal.longValueExact();
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            throw new IllegalArgumentException("Expected a numeric value");
        }

        private int asInt(Object value) {
            if (value instanceof BigDecimal decimal) {
                return decimal.intValueExact();
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            throw new IllegalArgumentException("Expected a numeric value");
        }

        private long parseId(String path, String prefix) {
            String tail = path.substring(prefix.length());
            if (tail.isBlank()) {
                throw new IllegalArgumentException("Missing resource id");
            }
            return Long.parseLong(tail);
        }

        private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
            byte[] response = Json.stringify(payload).getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

        private void sendNoContent(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private void addCorsHeaders(Headers headers) {
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type");
        }
    }
}
