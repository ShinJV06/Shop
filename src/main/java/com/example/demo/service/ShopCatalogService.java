package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.*;

@Service
public class ShopCatalogService {
    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    @Autowired
    private ProductRepository productRepository;

    public List<Product> visibleProducts() {
        return productRepository.findByVisibleTrueOrderByIdAsc();
    }

    public Optional<Product> findBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    public Map<String, Object> buildCartSummary(Map<String, Integer> cart) {
        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0L;
        int count = 0;

        List<Product> products = productRepository.findAll();
        Map<String, Product> bySlug = new HashMap<>();
        for (Product p : products) {
            bySlug.put(p.getSlug(), p);
        }

        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            Product product = bySlug.get(e.getKey());
            if (product == null || !product.isVisible()) {
                continue;
            }
            int quantity = e.getValue();
            if (quantity <= 0) {
                continue;
            }
            long subtotal = product.getPrice() * quantity;
            total += subtotal;
            count += quantity;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", product.getSlug());
            item.put("name", product.getName());
            item.put("quantity", quantity);
            item.put("price", formatPrice(product.getPrice()));
            item.put("unitAmount", product.getPrice());
            item.put("subtotal", formatPrice(subtotal));
            item.put("subtotalAmount", subtotal);
            items.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("count", count);
        summary.put("total", formatPrice(total));
        summary.put("totalAmount", total);
        summary.put("items", items);
        summary.put("empty", items.isEmpty());
        return summary;
    }

    public String formatPrice(long amount) {
        return NumberFormat.getNumberInstance(VI_LOCALE).format(amount) + "đ";
    }

    public String formatPrice(double amount) {
        return NumberFormat.getNumberInstance(VI_LOCALE).format((long) amount) + "đ";
    }
}
