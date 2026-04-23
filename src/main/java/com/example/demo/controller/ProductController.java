package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ShopCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductController {

    private static final String CART_SESSION_KEY = "cart";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @GetMapping("/product/{slug}")
    public String productDetail(
            @PathVariable String slug,
            Model model,
            HttpSession session
    ) {
        var productOpt = productRepository.findBySlugIgnoreCase(slug);
        if (productOpt.isEmpty() || !productOpt.get().isVisible()) {
            return "redirect:/";
        }

        Product product = productOpt.get();
        model.addAttribute("product", product);

        // Lấy tất cả inventory items của product này (đã duyệt, không SOLD)
        List<InventoryItem> accounts = inventoryItemRepository
            .findByModerationStatusAndHiddenFalseOrderByIdDesc(ModerationStatus.APPROVED)
            .stream()
            .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
            .filter(item -> item.getStatus() != InventoryItemStatus.SOLD)
            .toList();
        model.addAttribute("accounts", accounts);

        // Format prices
        Map<Long, String> accountPrices = new HashMap<>();
        for (InventoryItem acc : accounts) {
            if (acc.getProduct() != null) {
                accountPrices.put(acc.getId(), shopCatalogService.formatItemPrice(acc));
            }
        }
        model.addAttribute("accountPrices", accountPrices);

        // Cart summary
        model.addAttribute("cartSummary", shopCatalogService.buildCartSummary(getOrCreateCart(session)));
        
        // Compare count
        @SuppressWarnings("unchecked")
        List<String> compareList = (List<String>) session.getAttribute("compareList");
        model.addAttribute("compareCount", compareList != null ? compareList.size() : 0);

        return "product-detail";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getOrCreateCart(HttpSession session) {
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        if (cartObject == null) {
            return new HashMap<>();
        }
        if (cartObject instanceof Map<?, ?> rawCart) {
            Map<String, Integer> cart = new HashMap<>();
            rawCart.forEach((key, value) -> {
                if (key instanceof String productId) {
                    int qty = 0;
                    if (value instanceof Integer i) {
                        qty = i;
                    } else if (value instanceof Number n) {
                        qty = n.intValue();
                    } else if (value instanceof String s) {
                        try { qty = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
                    }
                    if (qty > 0) {
                        cart.put(productId, qty);
                    }
                }
            });
            return cart;
        }
        return new HashMap<>();
    }
}
