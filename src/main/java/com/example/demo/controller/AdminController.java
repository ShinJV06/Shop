package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.Enum.Role;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.ShopOrder;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.service.AdminUserService;
import com.example.demo.service.DashboardStatsService;
import com.example.demo.service.InventoryAdminService;
import com.example.demo.service.OrderFlowService;
import com.example.demo.service.ShopCatalogService;
import com.example.demo.web.CurrentUserService;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DashboardStatsService dashboardStatsService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdminService inventoryAdminService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private OrderFlowService orderFlowService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @GetMapping
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        model.addAttribute("stats", dashboardStatsService.overview());
        return "admin/dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByIdDesc();
        model.addAttribute("items", items);
        Set<Long> submitterIds = items.stream()
                .map(InventoryItem::getSubmittedById)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> submitterNames = new HashMap<>();
        if (!submitterIds.isEmpty()) {
            accountRepository.findAllById(submitterIds).forEach(a -> submitterNames.put(a.getId(), a.getUsername()));
        }
        model.addAttribute("submitterNames", submitterNames);
        Map<Long, String> priceByProductId = new LinkedHashMap<>();
        for (Product p : productRepository.findAll()) {
            priceByProductId.put(p.getId(), shopCatalogService.formatPrice(p.getPrice()));
        }
        model.addAttribute("priceByProductId", priceByProductId);

        // Price for each inventory item (custom price or product price)
        Map<Long, String> itemPriceMap = new LinkedHashMap<>();
        for (InventoryItem item : items) {
            itemPriceMap.put(item.getId(), shopCatalogService.formatItemPrice(item));
        }
        model.addAttribute("itemPriceMap", itemPriceMap);
        return "admin/accounts";
    }

    @GetMapping("/inventory")
    public String inventoryLegacy() {
        return "redirect:/admin/stock";
    }

    @GetMapping("/products")
    public String productsPage(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        model.addAttribute("products", productRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/stock")
    public String stockPage(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        model.addAttribute("products", productRepository.findAll());
        return "admin/stock";
    }

    @GetMapping("/pending")
    public String pendingLegacy() {
        return "redirect:/admin/accounts";
    }

    @GetMapping("/users")
    public String users(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        model.addAttribute("users", adminUserService.listAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            adminUserService.setLocked(id, true, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã khóa user.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            adminUserService.setLocked(id, false, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã mở khóa user.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPw(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            String temp = adminUserService.resetPassword(id, admin.getId());
            ra.addFlashAttribute("successMessage", "Mật khẩu mới (chỉ hiện 1 lần): " + temp);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable long id, @RequestParam Role role, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            adminUserService.setRole(id, role, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã cập nhật role.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/orders")
    public String userOrders(@PathVariable long id, HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        model.addAttribute("targetUserId", id);
        model.addAttribute("orders", adminUserService.purchaseHistory(id));
        return "admin/user-orders";
    }

    @PostMapping("/inventory/add")
    public String addInventory(
            @RequestParam long productId,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String rankInfo,
            @RequestParam(required = false) String skinInfo,
            @RequestParam(required = false) String extraInfo,
            @RequestParam(required = false) BigDecimal customPrice,
            @RequestParam String credentials,
            @RequestParam(required = false) MultipartFile listingImage,
            HttpSession session,
            RedirectAttributes ra
    ) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.addSingle(productId, game, rankInfo, skinInfo, extraInfo, credentials, admin, listingImage, customPrice);
            ra.addFlashAttribute("successMessage", "Đã thêm acc vào kho.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/stock";
    }

    @PostMapping("/inventory/import")
    public String importInventory(
            @RequestParam long productId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String defaultGame,
            @RequestParam(required = false) String defaultRank,
            HttpSession session,
            RedirectAttributes ra
    ) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Chọn file txt/csv.");
            return "redirect:/admin/stock";
        }
        try {
            int n = inventoryAdminService.importBulk(productId, file.getInputStream(), defaultGame, defaultRank, admin);
            ra.addFlashAttribute("successMessage", "Import thành công " + n + " acc.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/stock";
    }

    @PostMapping("/inventory/{id}/hidden")
    public String setHidden(
            @PathVariable long id,
            @RequestParam boolean hidden,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.setHidden(id, hidden);
            ra.addFlashAttribute("successMessage", "Đã cập nhật ẩn/hiện.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/inventory/{id}/status")
    public String setStatus(
            @PathVariable long id,
            @RequestParam InventoryItemStatus status,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.setStatus(id, status);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái acc.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(
            @PathVariable long id,
            @RequestParam long price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") boolean visible,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(price);
        if (description != null) {
            p.setDescription(description);
        }
        p.setVisible(visible);
        productRepository.save(p);
        ra.addFlashAttribute("successMessage", "Đã lưu sản phẩm.");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(
            @PathVariable long id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        Product p = productRepository.findById(id).orElseThrow();
        // Xóa các inventory items liên quan trước
        List<InventoryItem> items = inventoryItemRepository.findAll().stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId().equals(id))
                .toList();
        inventoryItemRepository.deleteAll(items);
        // Xóa sản phẩm
        productRepository.delete(p);
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm và các acc liên quan.");
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String orders(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        List<ShopOrder> allOrders = shopOrderRepository.findAllByOrderByIdDesc();
        List<ShopOrder> pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
                .collect(Collectors.toList());
        List<ShopOrder> paidOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(Collectors.toList());

        Map<Long, String> buyerNames = new HashMap<>();
        Set<Long> buyerIds = allOrders.stream()
                .map(ShopOrder::getBuyerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!buyerIds.isEmpty()) {
            accountRepository.findAllById(buyerIds).forEach(a -> buyerNames.put(a.getId(), a.getUsername()));
        }

        model.addAttribute("allOrders", allOrders);
        model.addAttribute("buyerNames", buyerNames);
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable long id, HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
        ShopOrder order = shopOrderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        model.addAttribute("logs", orderFlowService.logsForOrder(id));
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/confirm-payment")
    public String confirm(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            orderFlowService.confirmPayment(id, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã xác nhận thanh toán và giao acc ngẫu nhiên.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/refund")
    public String refund(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account admin = requireAdmin(session, ra);
        if (admin == null) {
            return "redirect:/auth/login";
        }
        try {
            orderFlowService.refundOrder(id, admin.getId());
            ra.addFlashAttribute("successMessage", "Đã hoàn tiền / thu hồi acc về kho (nếu có).");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/pending/{id}/approve")
    public String approve(
            @PathVariable long id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.approve(id);
            ra.addFlashAttribute("successMessage", "Đã duyệt acc.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/pending/{id}/reject")
    public String reject(
            @PathVariable long id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (requireAdmin(session, ra) == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.reject(id);
            ra.addFlashAttribute("successMessage", "Đã từ chối.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    private Account requireAdmin(HttpSession session, RedirectAttributes ra) {
        try {
            return currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return null;
        }
    }
}
