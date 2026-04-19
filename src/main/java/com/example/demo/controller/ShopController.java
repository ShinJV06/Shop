package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.ShopOrder;
import com.example.demo.model.AddToCartRequest;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import com.example.demo.service.OrderFlowService;
import com.example.demo.service.ShopCatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ShopController {

    private static final String CART_SESSION_KEY = "cart";

    @Autowired
    private ShopCatalogService shopCatalogService;

    @Autowired
    private OrderFlowService orderFlowService;

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @GetMapping("/")
    public String homePage(Model model, HttpSession session) {
        model.addAttribute("cartSummary", buildCartSummary(session));

        // Top nạp tiền
        List<Account> topDepositors = accountRepository.findTopDepositors(5);
        List<Map<String, Object>> topDepositorList = new ArrayList<>();
        for (Account acc : topDepositors) {
            Double total = transactionLogEntryRepository.totalDepositsByAccountId(acc.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", acc.getUsername());
            item.put("total", shopCatalogService.formatPrice(total != null ? total : 0.0));
            item.put("avatar", getAvatarInitial(acc.getUsername()));
            topDepositorList.add(item);
        }
        model.addAttribute("topDepositors", topDepositorList);

        // Lấy acc đã duyệt, không bị ẩn, có sẵn - hiển thị theo kho acc
        List<InventoryItem> visibleAccounts = inventoryItemRepository
            .findByModerationStatusAndHiddenFalseOrderByIdDesc(ModerationStatus.APPROVED);
        model.addAttribute("visibleAccounts", visibleAccounts);

        // Map giá theo productId cho hiển thị
        Map<Long, String> accountPriceFormatted = new LinkedHashMap<>();
        // JSON data cho modal chi tiết
        Map<Long, Map<String, String>> accountJsonMap = new LinkedHashMap<>();

        for (InventoryItem item : visibleAccounts) {
            if (item.getProduct() != null) {
                accountPriceFormatted.put(item.getId(), shopCatalogService.formatPrice(item.getProduct().getPrice()));

                Map<String, String> jsonItem = new HashMap<>();
                jsonItem.put("id", String.valueOf(item.getId()));
                jsonItem.put("name", item.getProduct().getName() != null ? item.getProduct().getName() : "Acc Genshin");
                jsonItem.put("slug", item.getProduct().getSlug() != null ? item.getProduct().getSlug() : "");
                jsonItem.put("price", shopCatalogService.formatPrice(item.getProduct().getPrice()));
                jsonItem.put("priceSlug", item.getProduct().getSlug() != null ? item.getProduct().getSlug() : "");
                jsonItem.put("rank", item.getRankInfo() != null ? item.getRankInfo() : "-");
                jsonItem.put("skin", item.getSkinInfo() != null ? item.getSkinInfo() : "-");
                jsonItem.put("game", item.getGame() != null ? item.getGame() : "-");
                jsonItem.put("extra", item.getExtraInfo() != null ? item.getExtraInfo() : "");
                String imgPath = (item.getListingImagePath() != null && !item.getListingImagePath().isEmpty())
                    ? item.getListingImagePath()
                    : (item.getProduct().getImagePath() != null ? item.getProduct().getImagePath() : "");
                jsonItem.put("image", imgPath);
                String tagClass = "";
                if (item.getProduct().getSlug() != null) {
                    if (item.getProduct().getSlug().contains("premium")) tagClass = "purple";
                    else if (item.getProduct().getSlug().contains("vip")) tagClass = "gold";
                }
                jsonItem.put("tagClass", tagClass);
                accountJsonMap.put(item.getId(), jsonItem);
            } else {
                accountPriceFormatted.put(item.getId(), "Liên hệ");
            }
        }
        model.addAttribute("accountPriceFormatted", accountPriceFormatted);
        try {
            model.addAttribute("accountJson", new ObjectMapper().writeValueAsString(accountJsonMap));
        } catch (JsonProcessingException e) {
            model.addAttribute("accountJson", "{}");
        }

        String role = (String) session.getAttribute("role");
        Long userId = (Long) session.getAttribute("userId");
        if ("ADMIN".equals(role)) {
            List<ShopOrder> pendingPay = shopOrderRepository.findTop8ByStatusOrderByIdDesc(OrderStatus.PENDING_PAYMENT);
            List<InventoryItem> pendingSub = inventoryItemRepository.findTop6ByModerationStatusOrderByIdDesc(ModerationStatus.PENDING);
            model.addAttribute("homeAdminPendingOrders", pendingPay);
            model.addAttribute("homeAdminPendingSubmissions", pendingSub);
            Set<Long> nameIds = pendingPay.stream().map(ShopOrder::getBuyerId).filter(Objects::nonNull).collect(Collectors.toSet());
            pendingSub.stream().map(InventoryItem::getSubmittedById).filter(Objects::nonNull).forEach(nameIds::add);
            Map<Long, String> names = new HashMap<>();
            if (!nameIds.isEmpty()) {
                accountRepository.findAllById(nameIds).forEach(a -> names.put(a.getId(), a.getUsername()));
            }
            model.addAttribute("homeAdminNameByAccountId", names);
        }
        if (userId != null && !"ADMIN".equals(role)) {
            model.addAttribute("homeUserPendingOrders", shopOrderRepository.findTop5ByBuyerIdAndStatusOrderByIdDesc(userId, OrderStatus.PENDING_PAYMENT));
        }
        return "home";
    }

    @GetMapping("/cart")
    public String cartPage(Model model, HttpSession session) {
        model.addAttribute("cartSummary", buildCartSummary(session));
        return "cart";
    }

    @GetMapping("/my-orders")
    public String myOrders(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            ra.addFlashAttribute("errorMessage", "Đăng nhập để xem lịch sử mua hàng.");
            return "redirect:/auth/login";
        }
        model.addAttribute("orders", shopOrderRepository.findByBuyerIdOrderByIdDesc(userId));
        return "my-orders";
    }

    @GetMapping("/my-orders/{id}")
    public String myOrderDetail(
            @PathVariable long id,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            ra.addFlashAttribute("errorMessage", "Đăng nhập để xem đơn hàng.");
            return "redirect:/auth/login";
        }
        ShopOrder order = shopOrderRepository.findById(id).orElse(null);
        if (order == null || !order.getBuyerId().equals(userId)) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/my-orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("logs", orderFlowService.logsForOrder(id));
        return "my-order-detail";
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody AddToCartRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bạn cần đăng nhập trước khi thêm acc vào giỏ hàng."));
        }
        if (request == null || request.getProductId() == null || request.getProductId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mã sản phẩm."));
        }

        var productOptional = shopCatalogService.findBySlug(request.getProductId().trim());
        if (productOptional.isEmpty() || !productOptional.get().isVisible()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy sản phẩm."));
        }

        Product product = productOptional.get();
        Map<String, Integer> cart = getOrCreateCart(session);
        if (cart.containsKey(product.getSlug())) {
            Map<String, Object> summary = buildCartSummary(session);
            summary.put("message", product.getName() + " đã có sẵn trong giỏ hàng.");
            return ResponseEntity.ok(summary);
        }
        cart.put(product.getSlug(), 1);
        session.setAttribute(CART_SESSION_KEY, cart);

        Map<String, Object> summary = buildCartSummary(session);
        summary.put("message", "Đã thêm " + product.getName() + " vào giỏ hàng.");
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(@RequestBody AddToCartRequest request, HttpSession session) {
        if (request == null || request.getProductId() == null || request.getProductId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mã sản phẩm."));
        }

        Map<String, Integer> cart = getOrCreateCart(session);
        cart.remove(request.getProductId());
        session.setAttribute(CART_SESSION_KEY, cart);

        Map<String, Object> summary = buildCartSummary(session);
        summary.put("message", "Đã xóa acc khỏi giỏ hàng.");
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        session.setAttribute(CART_SESSION_KEY, new LinkedHashMap<String, Integer>());

        Map<String, Object> summary = buildCartSummary(session);
        summary.put("message", "Đã xóa toàn bộ giỏ hàng.");
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/orders/create")
    @ResponseBody
    public ResponseEntity<?> createOrder(@RequestBody Map<String, List<String>> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Cần đăng nhập."));
        }
        List<String> slugs = body == null ? null : body.get("productSlugs");
        if (slugs == null || slugs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chọn ít nhất một acc."));
        }
        Map<String, Integer> cart = getOrCreateCart(session);
        for (String slug : slugs) {
            if (!cart.containsKey(slug) || cart.get(slug) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Giỏ hàng không khớp (thiếu " + slug + "). Hãy tải lại trang."));
            }
        }
        try {
            var order = orderFlowService.createOrderFromSlugs(userId, slugs);
            for (String slug : slugs) {
                cart.remove(slug);
            }
            session.setAttribute(CART_SESSION_KEY, cart);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderId", order.getId());
            res.put("totalAmount", order.getTotalAmount());
            res.put("total", shopCatalogService.formatPrice(order.getTotalAmount()));
            res.put("paymentNote", "DON_SO_" + order.getId());
            res.put("cartSummary", buildCartSummary(session));
            res.put("message", "Đã tạo đơn #" + order.getId() + ". Quét QR với nội dung ghi đúng mã đơn.");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getOrCreateCart(HttpSession session) {
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        if (cartObject instanceof Map<?, ?> rawCart) {
            Map<String, Integer> cart = new LinkedHashMap<>();
            rawCart.forEach((key, value) -> {
                if (key instanceof String productId && value instanceof Integer quantity) {
                    cart.put(productId, quantity);
                }
            });
            return cart;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildCartSummary(HttpSession session) {
        return shopCatalogService.buildCartSummary(getOrCreateCart(session));
    }

    private String getAvatarInitial(String username) {
        if (username == null || username.isEmpty()) return "?";
        return username.substring(0, 1).toUpperCase();
    }
}
