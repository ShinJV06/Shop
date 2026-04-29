package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.Game;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.MysteryBag;
import com.example.demo.entity.Product;
import com.example.demo.entity.ShopOrder;
import com.example.demo.model.AddToCartRequest;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.MysteryBagRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import com.example.demo.service.GameService;
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

import java.util.*;
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
    private ProductRepository productRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private MysteryBagRepository mysteryBagRepository;

    @GetMapping("/")
    public String homePage(Model model, HttpSession session) {
        model.addAttribute("cartSummary", buildCartSummary(session));

        // Danh sách game
        List<Game> games = gameService.getAllVisibleGames();
        model.addAttribute("games", games);

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

        // Lấy acc đã duyệt, không bị ẩn, không phải SOLD - gộp theo product
        List<InventoryItem> visibleAccounts = inventoryItemRepository
            .findByModerationStatusAndHiddenFalseOrderByIdDesc(ModerationStatus.APPROVED)
            .stream()
            .filter(item -> item.getStatus() != InventoryItemStatus.SOLD)
            .collect(Collectors.toList());

        // Map product -> list accounts
        Map<Long, List<InventoryItem>> accountsByProduct = visibleAccounts.stream()
            .filter(item -> item.getProduct() != null)
            .collect(Collectors.groupingBy(item -> item.getProduct().getId()));

        // Lấy products có acc đã duyệt
        List<Product> productsWithAccounts = visibleAccounts.stream()
            .filter(item -> item.getProduct() != null)
            .map(InventoryItem::getProduct)
            .distinct()
            .toList();

        // Lấy tất cả products visible từ DB
        var allProducts = productRepository.findByVisibleTrueOrderByIdAsc();
        List<Product> uniqueProducts;
        if (productsWithAccounts.isEmpty() && !allProducts.isEmpty()) {
            uniqueProducts = allProducts;
        } else {
            uniqueProducts = productsWithAccounts;
        }

        // Đếm số acc mỗi product
        Map<Long, Integer> productAccCount = new HashMap<>();
        for (Map.Entry<Long, List<InventoryItem>> entry : accountsByProduct.entrySet()) {
            productAccCount.put(entry.getKey(), entry.getValue().size());
        }

        model.addAttribute("uniqueProducts", uniqueProducts);
        model.addAttribute("productAccCount", productAccCount);
        model.addAttribute("accountsByProduct", accountsByProduct);

        // Map game -> list accounts (cho hiển thị trên home page)
        Map<Long, List<InventoryItem>> gameAccountsMap = visibleAccounts.stream()
            .filter(item -> item.getProduct() != null && item.getProduct().getGame() != null)
            .collect(Collectors.groupingBy(item -> item.getProduct().getGame().getId()));
        model.addAttribute("gameAccountsMap", gameAccountsMap);

        // Map account id -> formatted price
        Map<Long, String> accountPriceMap = new HashMap<>();
        for (InventoryItem acc : visibleAccounts) {
            if (acc.getProduct() != null) {
                accountPriceMap.put(acc.getId(), shopCatalogService.formatItemPrice(acc));
            }
        }
        model.addAttribute("accountPriceMap", accountPriceMap);
        model.addAttribute("visibleAccounts", visibleAccounts);

        // Nhóm products theo game - CHỈ hiện game có accounts đã duyệt
        // Sắp xếp: Genshin Impact đầu, Liên Quân thứ 2, sau đó theo thứ tự khác
        Map<Game, List<Product>> tempMap = new LinkedHashMap<>();
        Game genshinGame = null;
        Game lienquanGame = null;
        List<Product> genshinProducts = new ArrayList<>();
        List<Product> lienquanProducts = new ArrayList<>();

        for (Product p : uniqueProducts) {
            if (p.getGame() != null) {
                // Chỉ thêm product nếu có acc đã duyệt
                List<InventoryItem> accs = accountsByProduct.get(p.getId());
                if (accs == null || accs.isEmpty()) continue;
                
                String gameName = p.getGame().getName().toLowerCase();
                if (gameName.contains("genshin")) {
                    genshinGame = p.getGame();
                    genshinProducts.add(p);
                } else if (gameName.contains("liên quân") || gameName.contains("lien quan")) {
                    lienquanGame = p.getGame();
                    lienquanProducts.add(p);
                } else {
                    tempMap.computeIfAbsent(p.getGame(), k -> new ArrayList<>()).add(p);
                }
            }
        }

        // Tạo map mới: Genshin Impact đầu, Liên Quân thứ 2
        Map<Game, List<Product>> productsByGame = new LinkedHashMap<>();
        if (genshinGame != null && !genshinProducts.isEmpty()) {
            productsByGame.put(genshinGame, genshinProducts);
        }
        if (lienquanGame != null && !lienquanProducts.isEmpty()) {
            productsByGame.put(lienquanGame, lienquanProducts);
        }
        productsByGame.putAll(tempMap);

        // Bỏ phần "Không phân loại" - chỉ hiện game có products

        // Đếm số acc mỗi game
        Map<Long, Integer> gameAccCount = new HashMap<>();
        Map<Integer, Integer> noGameAccCount = new HashMap<>();
        for (Game game : productsByGame.keySet()) {
            int total = 0;
            for (Product p : productsByGame.get(game)) {
                total += productAccCount.getOrDefault(p.getId(), 0);
            }
            if (game.getId() == -1L) {
                noGameAccCount.put(-1, total);
            } else {
                gameAccCount.put(game.getId(), total);
            }
        }

        model.addAttribute("productsByGame", productsByGame);
        model.addAttribute("gameAccCount", gameAccCount);
        model.addAttribute("noGameAccCount", noGameAccCount);

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
        if (userId != null) {
            var accountOpt = accountRepository.findById(userId);
            if (accountOpt.isPresent()) {
                double balance = accountOpt.get().getWallet();
                model.addAttribute("walletBalance", shopCatalogService.formatPrice(Math.max(0, balance)));
                session.setAttribute("wallet", shopCatalogService.formatPrice(Math.max(0, balance)));
            }
        }
        if (userId != null && !"ADMIN".equals(role)) {
            model.addAttribute("homeUserPendingOrders", shopOrderRepository.findTop5ByBuyerIdAndStatusOrderByIdDesc(userId, OrderStatus.PENDING_PAYMENT));
        }
        
        // Compare count
        @SuppressWarnings("unchecked")
        List<String> compareList = (List<String>) session.getAttribute("compareList");
        model.addAttribute("compareCount", compareList != null ? compareList.size() : 0);
        
        return "home";
    }

    @GetMapping("/cart")
    public String cartPage(Model model, HttpSession session) {
        model.addAttribute("cartSummary", buildCartSummary(session));
        
        @SuppressWarnings("unchecked")
        List<String> compareList = (List<String>) session.getAttribute("compareList");
        model.addAttribute("compareCount", compareList != null ? compareList.size() : 0);
        
        return "cart";
    }

    @GetMapping("/contact")
    public String contactPage() {
        return "contact";
    }

    @GetMapping("/my-orders")
    public String myOrders(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            ra.addFlashAttribute("errorMessage", "Đăng nhập để xem lịch sử mua hàng.");
            return "redirect:/auth/login";
        }
        model.addAttribute("orders", shopOrderRepository.findByBuyerIdAndStatusOrderByIdDesc(userId, OrderStatus.PAID));
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

    @GetMapping("/minigame")
    public String minigamePage() {
        return "minigame.html";
    }

    // DEBUG: Test mystery bag API directly
    @GetMapping("/debug/mystery-bags")
    @ResponseBody
    public ResponseEntity<?> debugMysteryBags() {
        try {
            var bags = mysteryBagRepository.findAllActive();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", bags.size(),
                "bags", bags
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/cart/buy-now")
    @ResponseBody
    public ResponseEntity<?> buyNow(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bạn cần đăng nhập trước khi mua."));
        }
        
        String productSlug = body != null ? (String) body.get("productId") : null;
        Object invItemIdObj = body != null ? body.get("inventoryItemId") : null;
        Long inventoryItemId = null;
        if (invItemIdObj != null) {
            if (invItemIdObj instanceof Number) {
                inventoryItemId = ((Number) invItemIdObj).longValue();
            } else {
                inventoryItemId = Long.parseLong(invItemIdObj.toString());
            }
        }

        if (productSlug == null || productSlug.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mã sản phẩm."));
        }

        var productOpt = shopCatalogService.findBySlug(productSlug.trim());
        if (productOpt.isEmpty() || !productOpt.get().isVisible()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy sản phẩm."));
        }

        try {
            var order = orderFlowService.createOrderFromSlugs(userId, List.of(productSlug), "WALLET");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thanh toán thành công!",
                "redirectUrl", "/my-orders/" + order.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
        String paymentMethod = "WALLET"; // default
        if (body != null && body.containsKey("paymentMethod") && !body.get("paymentMethod").isEmpty()) {
            paymentMethod = body.get("paymentMethod").get(0);
        }
        for (String slug : slugs) {
            if (!cart.containsKey(slug) || cart.get(slug) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Giỏ hàng không khớp (thiếu " + slug + "). Hãy tải lại trang."));
            }
        }
        try {
            var order = orderFlowService.createOrderFromSlugs(userId, slugs, paymentMethod);
            for (String slug : slugs) {
                cart.remove(slug);
            }
            session.setAttribute(CART_SESSION_KEY, cart);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderId", order.getId());
            res.put("totalAmount", order.getTotalAmount());
            res.put("total", shopCatalogService.formatPrice(order.getTotalAmount()));
            res.put("paymentMethod", paymentMethod);
            res.put("paymentNote", "DON_SO_" + order.getId());
            res.put("cartSummary", buildCartSummary(session));
            String msg = paymentMethod.equals("WALLET")
                ? "Thanh toán thành công! Acc đã được gửi vào email."
                : "Đã tạo đơn #" + order.getId() + ". Quét QR với nội dung ghi đúng mã đơn.";
            res.put("message", msg);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getOrCreateCart(HttpSession session) {
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        if (cartObject == null) {
            return new LinkedHashMap<>();
        }
        if (cartObject instanceof Map<?, ?> rawCart) {
            Map<String, Integer> cart = new LinkedHashMap<>();
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
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildCartSummary(HttpSession session) {
        return shopCatalogService.buildCartSummary(getOrCreateCart(session));
    }

    @GetMapping("/explore/{gameSlug}")
    public String exploreGame(
            @PathVariable String gameSlug,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String arLevel,
            @RequestParam(required = false) String server,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model,
            HttpSession session
    ) {
        model.addAttribute("cartSummary", buildCartSummary(session));
        
        // Tìm game theo slug
        Optional<Game> gameOpt = gameService.findBySlug(gameSlug);
        if (gameOpt.isEmpty()) {
            return "redirect:/";
        }
        Game game = gameOpt.get();
        model.addAttribute("currentGame", game);

        // Danh sách game cho sidebar
        List<Game> allGames = gameService.getAllVisibleGames();
        model.addAttribute("games", allGames);

        // Lấy tất cả inventory items đã duyệt của game này
        List<InventoryItem> allAccounts = inventoryItemRepository
            .findByGameIdAndModerationStatusAndHiddenFalse(game.getId())
            .stream()
            .filter(acc -> acc.getStatus() != InventoryItemStatus.SOLD)
            .collect(Collectors.toList());

        // Lọc theo gói acc (product)
        if (product != null && !product.isBlank()) {
            String productSlug = product.toLowerCase();
            allAccounts = allAccounts.stream()
                .filter(acc -> acc.getProduct() != null && acc.getProduct().getSlug().toLowerCase().equals(productSlug))
                .collect(Collectors.toList());
        }

        // Lọc theo tìm kiếm
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            allAccounts = allAccounts.stream()
                .filter(acc -> {
                    String name = acc.getProduct() != null ? acc.getProduct().getName().toLowerCase() : "";
                    String notes = acc.getExtraInfo() != null ? acc.getExtraInfo().toLowerCase() : "";
                    String rankInfo = acc.getRankInfo() != null ? acc.getRankInfo().toLowerCase() : "";
                    return name.contains(searchLower) || notes.contains(searchLower) || rankInfo.contains(searchLower);
                })
                .collect(Collectors.toList());
        }

        // Lọc theo AR Level
        if (arLevel != null && !arLevel.isBlank() && !arLevel.equals("all")) {
            allAccounts = allAccounts.stream()
                .filter(acc -> {
                    if (acc.getRankInfo() == null) return false;
                    try {
                        // Parse AR level từ rankInfo (ví dụ: "AR 58", "AR45")
                        String rankInfo = acc.getRankInfo().toLowerCase();
                        int ar = 0;
                        if (rankInfo.contains("ar")) {
                            String arStr = rankInfo.replaceAll("[^0-9]", "");
                            if (!arStr.isEmpty()) {
                                ar = Integer.parseInt(arStr);
                            }
                        }
                        switch (arLevel) {
                            case "1-20": return ar >= 1 && ar <= 20;
                            case "20-40": return ar >= 21 && ar <= 40;
                            case "40-60": return ar >= 41 && ar <= 60;
                            default: return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        }

        // Lọc theo Server
        if (server != null && !server.isBlank() && !server.equals("all")) {
            final String serverFilter = server;
            allAccounts = allAccounts.stream()
                .filter(acc -> acc.getGame() != null && acc.getGame().toLowerCase().contains(serverFilter.toLowerCase()))
                .collect(Collectors.toList());
        }

        // Lọc theo giá
        if ((minPrice != null && !minPrice.isBlank()) || (maxPrice != null && !maxPrice.isBlank())) {
            final double min = minPrice != null && !minPrice.isBlank() ? Double.parseDouble(minPrice) : 0;
            final double max = maxPrice != null && !maxPrice.isBlank() ? Double.parseDouble(maxPrice) : Double.MAX_VALUE;
            allAccounts = allAccounts.stream()
                .filter(acc -> {
                    if (acc.getProduct() == null) return false;
                    double price = shopCatalogService.getEffectivePrice(acc);
                    return price >= min && price <= max;
                })
                .collect(Collectors.toList());
        }

        // Sắp xếp
        switch (sort) {
            case "price-asc":
                allAccounts.sort((a, b) -> {
                    double pa = shopCatalogService.getEffectivePrice(a);
                    double pb = shopCatalogService.getEffectivePrice(b);
                    return Double.compare(pa, pb);
                });
                break;
            case "price-desc":
                allAccounts.sort((a, b) -> {
                    double pa = shopCatalogService.getEffectivePrice(a);
                    double pb = shopCatalogService.getEffectivePrice(b);
                    return Double.compare(pb, pa);
                });
                break;
            default: // newest
                allAccounts.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        }

        // Phân trang
        int pageSize = 12;
        int totalAccounts = allAccounts.size();
        int totalPages = (int) Math.ceil((double) totalAccounts / pageSize);
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;
        
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalAccounts);
        List<InventoryItem> pagedAccounts = fromIndex < totalAccounts 
            ? allAccounts.subList(fromIndex, toIndex) 
            : List.of();

        model.addAttribute("accounts", pagedAccounts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalAccounts", totalAccounts);

        // Filter values
        model.addAttribute("searchFilter", search);
        model.addAttribute("arLevelFilter", arLevel);
        model.addAttribute("serverFilter", server);
        model.addAttribute("minPriceFilter", minPrice);
        model.addAttribute("maxPriceFilter", maxPrice);
        model.addAttribute("sortFilter", sort);
        model.addAttribute("selectedProduct", product);

        // Format giá
        Map<Long, String> accountPriceFormatted = new HashMap<>();
        for (InventoryItem acc : allAccounts) {
            accountPriceFormatted.put(acc.getId(), shopCatalogService.formatItemPrice(acc));
        }
        model.addAttribute("accountPriceFormatted", accountPriceFormatted);

        // JSON data cho JS
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> accountJson = new LinkedHashMap<>();
        for (InventoryItem acc : allAccounts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", acc.getProduct() != null ? acc.getProduct().getName() : "Acc");
            item.put("price", accountPriceFormatted.getOrDefault(acc.getId(), "Liên hệ"));
            item.put("priceFormatted", shopCatalogService.formatItemPrice(acc));
            item.put("priceSlug", acc.getProduct() != null ? acc.getProduct().getSlug() : "");
            item.put("image", acc.getListingImagePath() != null ? acc.getListingImagePath() 
                : (acc.getProduct() != null ? acc.getProduct().getImagePath() : ""));
            item.put("rank", acc.getRankInfo() != null ? acc.getRankInfo() : "-");
            item.put("server", acc.getGame() != null ? acc.getGame() : "-");
            item.put("game", game.getName());
            item.put("extra", acc.getExtraInfo() != null ? acc.getExtraInfo() : "");
            item.put("productSlug", acc.getProduct() != null ? acc.getProduct().getSlug() : "");
            accountJson.put(String.valueOf(acc.getId()), item);
        }
        try {
            model.addAttribute("accountJson", mapper.writeValueAsString(accountJson));
        } catch (JsonProcessingException e) {
            model.addAttribute("accountJson", "{}");
        }

        return "game-page";
    }

    @GetMapping("/account/{id}")
    public String accountDetail(
            @PathVariable Long id,
            Model model,
            HttpSession session
    ) {
        model.addAttribute("cartSummary", buildCartSummary(session));

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return "redirect:/";
        }
        InventoryItem acc = itemOpt.get();
        model.addAttribute("acc", acc);

        // Tính backUrl an toàn - dùng Product.game thay vì InventoryItem.game
        String backUrl = "/";
        if (acc.getProduct() != null && acc.getProduct().getGame() != null && acc.getProduct().getGame().getSlug() != null) {
            backUrl = "/explore/" + acc.getProduct().getGame().getSlug();
        }
        model.addAttribute("backUrl", backUrl);

        // Get related accounts (same product/game)
        if (acc.getProduct() != null && acc.getProduct().getGame() != null) {
            List<InventoryItem> related = inventoryItemRepository
                .findByModerationStatusAndHiddenFalseOrderByIdDesc(ModerationStatus.APPROVED)
                .stream()
                .filter(a -> a.getProduct() != null && a.getProduct().getGame() != null
                    && a.getProduct().getGame().getId().equals(acc.getProduct().getGame().getId())
                    && !a.getId().equals(id))
                .limit(4)
                .toList();
            model.addAttribute("relatedAccounts", related);
        }

        // Format price (dùng giá riêng nếu có)
        model.addAttribute("formattedPrice", shopCatalogService.formatItemPrice(acc));

        return "account-detail";
    }

    @GetMapping("/explore/{gameSlug}/accounts")
    @ResponseBody
    public ResponseEntity<?> getAccounts(
            @PathVariable String gameSlug,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String arLevel,
            @RequestParam(required = false) String server,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            HttpSession session
    ) {
        Optional<Game> gameOpt = gameService.findBySlug(gameSlug);
        if (gameOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Game game = gameOpt.get();

        List<InventoryItem> allAccounts = inventoryItemRepository
            .findByGameIdAndModerationStatusAndHiddenFalse(game.getId())
            .stream()
            .filter(acc -> acc.getStatus() != InventoryItemStatus.SOLD)
            .collect(Collectors.toList());

        // Apply filters (same as explore endpoint)
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            allAccounts = allAccounts.stream()
                .filter(acc -> {
                    String name = acc.getProduct() != null ? acc.getProduct().getName().toLowerCase() : "";
                    String notes = acc.getExtraInfo() != null ? acc.getExtraInfo().toLowerCase() : "";
                    return name.contains(searchLower) || notes.contains(searchLower);
                })
                .collect(Collectors.toList());
        }

        // Pagination
        int pageSize = 12;
        int totalAccounts = allAccounts.size();
        int totalPages = (int) Math.ceil((double) totalAccounts / pageSize);
        if (page < 1) page = 1;
        
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalAccounts);
        List<InventoryItem> pagedAccounts = fromIndex < totalAccounts 
            ? allAccounts.subList(fromIndex, toIndex) 
            : List.of();

        return ResponseEntity.ok(Map.of(
            "accounts", pagedAccounts,
            "currentPage", page,
            "totalPages", totalPages,
            "totalAccounts", totalAccounts
        ));
    }

    private String getAvatarInitial(String username) {
        if (username == null || username.isEmpty()) return "?";
        return username.substring(0, 1).toUpperCase();
    }
}
