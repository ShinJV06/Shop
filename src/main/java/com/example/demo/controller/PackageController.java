package com.example.demo.controller;

import com.example.demo.entity.Game;
import com.example.demo.entity.Product;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.web.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/packages")
public class PackageController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping
    public String list(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        List<Product> packages = productRepository.findAll();
        model.addAttribute("packages", packages);

        // Group by game
        Map<Game, List<Product>> byGame = packages.stream()
                .filter(p -> p.getGame() != null)
                .collect(Collectors.groupingBy(Product::getGame));
        model.addAttribute("byGame", byGame);

        // Count accounts per package
        Map<Long, Long> accCountByProduct = packages.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> inventoryItemRepository.countByProduct_IdAndStatusAndModerationStatusAndHiddenFalse(
                                p.getId(),
                                com.example.demo.entity.Enum.InventoryItemStatus.AVAILABLE,
                                com.example.demo.entity.Enum.ModerationStatus.APPROVED
                        )
                ));
        model.addAttribute("accCountByProduct", accCountByProduct);

        // Check if package has any accounts (for "create account" button)
        Map<Long, Boolean> hasAccounts = packages.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> inventoryItemRepository.countByProduct_IdAndStatusAndModerationStatusAndHiddenFalse(
                                p.getId(),
                                com.example.demo.entity.Enum.InventoryItemStatus.AVAILABLE,
                                com.example.demo.entity.Enum.ModerationStatus.APPROVED
                        ) > 0
                ));
        model.addAttribute("hasAccounts", hasAccounts);

        return "admin/packages";
    }

    @GetMapping("/create")
    public String createForm(HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        List<Game> games = gameRepository.findAllVisibleOrderByDisplayOrder();
        model.addAttribute("games", games);
        model.addAttribute("pkg", new Product());
        return "admin/package-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable long id, HttpSession session, Model model, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        Product pkg = productRepository.findById(id).orElse(null);
        if (pkg == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy gói acc!");
            return "redirect:/admin/packages";
        }

        List<Game> games = gameRepository.findAllVisibleOrderByDisplayOrder();
        model.addAttribute("games", games);
        model.addAttribute("pkg", pkg);
        return "admin/package-form";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String slug,
            @RequestParam Long gameId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) boolean visible,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        try {
            Product pkg;
            if (id != null && id > 0) {
                pkg = productRepository.findById(id).orElse(new Product());
            } else {
                pkg = new Product();
            }

            pkg.setName(name);
            pkg.setDescription(description);
            pkg.setVisible(visible);

            // Generate slug if empty
            if (slug == null || slug.isBlank()) {
                slug = name.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
            }
            pkg.setSlug(slug);

            // Set game (bắt buộc)
            gameRepository.findById(gameId).ifPresent(pkg::setGame);

            productRepository.save(pkg);
            ra.addFlashAttribute("successMessage", "Đã lưu gói acc: " + name);

        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/admin/packages";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        try {
            currentUserService.requireAdmin(session);
        } catch (CurrentUserService.UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }

        productRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Đã xóa gói acc!");
        return "redirect:/admin/packages";
    }
}
