package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Game;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.GameRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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

    @Autowired
    private GameRepository gameRepository;

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

        // Games for the game selector
        List<Game> games = gameRepository.findAllVisibleOrderByDisplayOrder();
        model.addAttribute("games", games);

        List<InventoryItem> mine = inventoryItemRepository.findAll().stream()
                .filter(i -> i.getSubmittedById() != null && i.getSubmittedById().equals(ctv.getId()))
                .filter(i -> i.getModerationStatus() == ModerationStatus.PENDING)
                .collect(Collectors.toList());
        model.addAttribute("myPendingCount", mine.size());
        return "ctv/submit";
    }

    @PostMapping("/submit")
    public String submit(
            @RequestParam long productId,
            @RequestParam String gameTab,
            @RequestParam String credentials,
            @RequestParam(required = false) String rankInfo,
            @RequestParam(required = false) String skinInfo,
            @RequestParam(required = false) String extraInfo,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String accountDesc,
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String server,
            @RequestParam(required = false) String swordOfSky,
            @RequestParam(required = false) String constellation,
            @RequestParam(required = false) String teamInfo,
            @RequestParam(required = false) String adminUser,
            @RequestParam(required = false) Long price,
            @RequestParam("listingImage") MultipartFile listingImage,
            HttpSession session,
            RedirectAttributes ra
    ) {
        Account ctv = requireCtv(session, ra);
        if (ctv == null) {
            return "redirect:/auth/login";
        }
        try {
            BigDecimal customPrice = (price != null && price > 0) ? new BigDecimal(price) : null;


            StringBuilder fullInfo = new StringBuilder();
            if (accountName != null && !accountName.isBlank()) fullInfo.append("Tên ACC: ").append(accountName).append("\n");
            if (accountDesc != null && !accountDesc.isBlank()) fullInfo.append("Mô tả: ").append(accountDesc).append("\n");
            if (uid != null && !uid.isBlank()) fullInfo.append("UID: ").append(uid).append("\n");
            if (accountType != null && !accountType.isBlank()) fullInfo.append("Loại ACC: ").append(accountType).append("\n");
            if (server != null && !server.isBlank()) fullInfo.append("Server: ").append(server).append("\n");
            if (swordOfSky != null && !swordOfSky.isBlank()) fullInfo.append("Thiên kiếm: ").append(swordOfSky).append("\n");
            if (constellation != null && !constellation.isBlank()) fullInfo.append("Constellation: ").append(constellation).append("\n");
            if (teamInfo != null && !teamInfo.isBlank()) fullInfo.append("Đội hình: ").append(teamInfo).append("\n");
            if (extraInfo != null && !extraInfo.isBlank()) fullInfo.append(extraInfo).append("\n");
            if (adminUser != null && !adminUser.isBlank()) fullInfo.append("TK Admin: ").append(adminUser);


            String gameName = null;
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null && product.getGame() != null) {
                gameName = product.getGame().getName();
            } else {
                gameName = gameTab;
            }

            inventoryAdminService.addSingle(productId, gameName, rankInfo, skinInfo, fullInfo.toString().trim(), credentials, ctv, listingImage, customPrice);
            ra.addFlashAttribute("successMessage", "Đã gửi — chờ admin duyệt.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/ctv/submit";
    }

    @GetMapping("/my-accounts")
    public String myAccounts(HttpSession session, Model model, RedirectAttributes ra) {
        Account ctv = requireCtv(session, ra);
        if (ctv == null) {
            return "redirect:/auth/login";
        }
        List<InventoryItem> mine = inventoryItemRepository.findAll().stream()
                .filter(i -> i.getSubmittedById() != null && i.getSubmittedById().equals(ctv.getId()))
                .collect(Collectors.toList());

        model.addAttribute("allItems", mine);
        model.addAttribute("pendingCount", mine.stream().filter(i -> i.getModerationStatus() == ModerationStatus.PENDING).count());
        model.addAttribute("approvedCount", mine.stream().filter(i -> i.getModerationStatus() == ModerationStatus.APPROVED).count());
        model.addAttribute("soldCount", mine.stream().filter(i -> i.getStatus() == InventoryItemStatus.SOLD).count());
        model.addAttribute("rejectedCount", mine.stream().filter(i -> i.getModerationStatus() == ModerationStatus.REJECTED).count());
        return "ctv/my-accounts";
    }

    @PostMapping("/my-accounts/delete/{id}")
    public String deleteAccount(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Account ctv = requireCtv(session, ra);
        if (ctv == null) {
            return "redirect:/auth/login";
        }
        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item != null && item.getSubmittedById() != null && item.getSubmittedById().equals(ctv.getId())
                && item.getModerationStatus() == ModerationStatus.PENDING) {
            inventoryItemRepository.delete(item);
            ra.addFlashAttribute("successMessage", "Đã xoá acc.");
        } else {
            ra.addFlashAttribute("errorMessage", "Không thể xoá acc này.");
        }
        return "redirect:/ctv/my-accounts";
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
