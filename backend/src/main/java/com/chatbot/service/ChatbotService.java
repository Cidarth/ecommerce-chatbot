package com.chatbot.service;

import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import java.io.FileReader;

@Service
public class ChatbotService {

    private List<Map<String, String>> products = new ArrayList<>();
    private List<Map<String, String>> orders = new ArrayList<>();
    private List<Map<String, String>> orderItems = new ArrayList<>();
    private List<Map<String, String>> inventoryItems = new ArrayList<>();
    private List<Map<String, String>> users = new ArrayList<>();
    private List<Map<String, String>> distributionCenters = new ArrayList<>();

    public ChatbotService() {
        loadCSV("ecommerce_dataset/products.csv", products);
        loadCSV("ecommerce_dataset/orders.csv", orders);
        loadCSV("ecommerce_dataset/order_items.csv", orderItems);
        loadCSV("ecommerce_dataset/inventory_items.csv", inventoryItems);
        loadCSV("ecommerce_dataset/users.csv", users);
        loadCSV("ecommerce_dataset/distribution_centers.csv", distributionCenters);
    }

    private void loadCSV(String filePath, List<Map<String, String>> targetList) {
        try {
            if (filePath.endsWith(".DS_Store")) return;
            CSVReader reader = new CSVReader(new FileReader(filePath));
            String[] headers = reader.readNext();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    map.put(headers[i], line[i]);
                }
                targetList.add(map);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error loading " + filePath + ": " + e.getMessage());
        }
    }

    public String processQuery(String question) {
        String q = question.toLowerCase();

        if (q.contains("top") && q.contains("sold")) {
            return getTopSellingProducts();
        } else if (q.contains("status") && q.contains("order")) {
            String id = q.replaceAll("\\D+", "");
            return getOrderStatus(id);
        } else if (q.contains("stock") || q.contains("how many")) {
            for (Map<String, String> p : products) {
                if (q.contains(p.get("product_name").toLowerCase())) {
                    return getStockLevel(p.get("product_name"));
                }
            }
            return "Product not found in inventory.";
        } else if (q.contains("stored") && q.contains("product id")) {
            String id = q.replaceAll("\\D+", "");
            return getStorageLocation(id);
        } else if (q.contains("who") && q.contains("order")) {
            String id = q.replaceAll("\\D+", "");
            return getUserForOrder(id);
        }

        return "Sorry, I didn't understand that.";
    }

    private String getTopSellingProducts() {
        Map<String, Integer> sales = new HashMap<>();
        for (Map<String, String> item : orderItems) {
            String pid = item.get("product_id");
            int qty = Integer.parseInt(item.get("quantity"));
            String name = products.stream()
                .filter(p -> p.get("product_id").equals(pid))
                .findFirst().map(p -> p.get("product_name")).orElse("Unknown");
            sales.put(name, sales.getOrDefault(name, 0) + qty);
        }
        return sales.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(5)
            .map(e -> e.getKey() + ": " + e.getValue() + " sold")
            .collect(Collectors.joining("\n"));
    }

    private String getOrderStatus(String id) {
        return orders.stream()
            .filter(o -> o.get("order_id").equals(id))
            .findFirst()
            .map(o -> "Order ID " + id + " is currently '" + o.get("order_status") + "'.")
            .orElse("Order ID not found.");
    }

    private String getStockLevel(String productName) {
        Optional<String> pidOpt = products.stream()
            .filter(p -> p.get("product_name").equalsIgnoreCase(productName))
            .map(p -> p.get("product_id")).findFirst();
        if (pidOpt.isPresent()) return "Product not found.";
        String pid = pidOpt.get();
        int totalStock = inventoryItems.stream()
            .filter(item -> item.get("product_id").equals(pid))
            .mapToInt(item -> Integer.parseInt(item.get("available_quantity")))
            .sum();
        return "There are " + totalStock + " units of " + productName + " left in stock.";
    }

    private String getStorageLocation(String productId) {
        List<String> dcIds = inventoryItems.stream()
            .filter(item -> item.get("product_id").equals(productId))
            .map(item -> item.get("distribution_center_id"))
            .distinct().collect(Collectors.toList());

        if (dcIds.isEmpty()) return "No storage found for product ID " + productId;

        List<String> names = distributionCenters.stream()
            .filter(dc -> dcIds.contains(dc.get("distribution_center_id")))
            .map(dc -> dc.get("name")).collect(Collectors.toList());

        return "Product ID " + productId + " is stored in: " + String.join(", ", names);
    }

    private String getUserForOrder(String orderId) {
        Optional<String> userId = orders.stream()
            .filter(o -> o.get("order_id").equals(orderId))
            .map(o -> o.get("user_id")).findFirst();
        if (userId.isPresent()) return "Order ID not found.";
        return users.stream()
            .filter(u -> u.get("user_id").equals(userId.get()))
            .map(u -> "Order ID " + orderId + " was placed by " + u.get("first_name") + " " + u.get("last_name"))
            .findFirst().orElse("User not found.");
    }
}