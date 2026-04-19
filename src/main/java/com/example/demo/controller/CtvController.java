package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.InventoryAdminService;
import com.example.demo.service.ShopCatalogService;
import com.example.demo.web.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ctv")
public class CtvController {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryAdminService inventoryAdminService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @GetMapping
    public String home(HttpSession session, RedirectAttributes ra) {
        if (requireCtv(session, ra) == null) {
            return "redirect:/auth/login";
        }
        return "redirect:/ctv/submit";
    }

    @GetMapping("/submit")
    public String submitForm(HttpSession session, Model model, RedirectAttributes ra) {
        Account ctv = requireCtv(session, ra);
        if (ctv == null) {
            return "redirect:/auth/login";
        }
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        Map<Long, String> productPriceFormatted = new LinkedHashMap<>();
        for (Product p : products) {
            productPriceFormatted.put(p.getId(), shopCatalogService.formatPrice(p.getPrice()));
        }
        model.addAttribute("productPriceFormatted", productPriceFormatted);
        List<InventoryItem> mine = inventoryItemRepository.findAll().stream()
                .filter(i -> i.getSubmittedById() != null && i.getSubmittedById().equals(ctv.getId()))
                .filter(i -> i.getModerationStatus() == ModerationStatus.PENDING)
                .collect(Collectors.toList());
        model.addAttribute("myPendingCount", mine.size());
        return "ctv/submit";
    }

    /**
     * Chỉ cần: gói acc + nội dung acc + ghi chú ngắn (tuỳ chọn).
     */
    @PostMapping("/submit")
    public String submit(
            @RequestParam long productId,
            @RequestParam String credentials,
            @RequestParam(required = false) String ghiChu,
            @RequestParam("listingImage") MultipartFile listingImage,
            HttpSession session,
            RedirectAttributes ra
    ) {
        Account ctv = requireCtv(session, ra);
        if (ctv == null) {
            return "redirect:/auth/login";
        }
        try {
            inventoryAdminService.addSingle(productId, null, null, null, ghiChu, credentials, ctv, listingImage);
            ra.addFlashAttribute("successMessage", "Đã gửi — chờ admin duyệt.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/ctv/submit";
    }

    private Account requireCtv(HttpSession session, RedirectAttributes ra) {
        try {
            return currentUserService.requireCtv(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return null;
        }
    }
}
