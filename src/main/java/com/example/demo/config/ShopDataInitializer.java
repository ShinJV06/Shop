package com.example.demo.config;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.Role;
import com.example.demo.entity.Game;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.util.CredentialHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class ShopDataInitializer implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed Games
        Game genshin = getOrCreateGame("genshin-impact", "Genshin Impact", "/images/genshin-banner.png", "Mua acc Genshin Impact giá rẻ");
        Game lienquan = getOrCreateGame("lien-quan", "Liên Quân Mobile", "/images/lienquan-banner.png", "Mua acc Liên Quân Mobile giá rẻ");

        // Seed Products
        if (productRepository.count() == 0) {
            // Genshin Products
            seedProduct("starter-archon", "Acc Khởi Đầu Archon", 199_000L,
                    "AR 25+, có nhân vật 5★ ngẫu nhiên. Phù hợp để bắt đầu hành trình Teyvat!",
                    "/images/genshin-1.png", genshin);
            seedProduct("waifu-premium", "Acc Waifu Premium", 499_000L,
                    "Full waifu meta, skin đẹp lung linh. Bộ sưu tập nhân vật nữ đỉnh nhất!",
                    "/images/genshin-1.png", genshin);
            seedProduct("endgame-vip", "Acc Endgame VIP", 1_290_000L,
                    "AR 55+, nhiều vũ khí trấn, đã clear abyss 36★. Dành cho game thủ đỉnh cao!",
                    "/images/genshin-1.png", genshin);

            // Liên Quân Products
            seedProduct("lq-dong-bac", "Acc Đồng/Bạc/Vàng", 99_000L,
                    "Tài khoản rank Đồng, Bạc, Vàng - Phù hợp cho người mới chơi.",
                    "/images/lienquan-banner.png", lienquan);
            seedProduct("lq-kim-cuong", "Acc Kim Cương", 299_000L,
                    "Tài khoản rank Kim Cương - Nhiều tướng, nhiều skin đẹp.",
                    "/images/lienquan-banner.png", lienquan);
            seedProduct("lq-cao-thu", "Acc Cao Thủ/Tinh Anh", 599_000L,
                    "Tài khoản rank Cao Thủ, Tinh Anh - Acc chất lượng cao.",
                    "/images/lienquan-banner.png", lienquan);
        }

        if (inventoryItemRepository.count() == 0) {
            for (Product product : productRepository.findAll()) {
                for (int i = 0; i < 2; i++) {
                    String cred = product.getSlug() + "|seed|" + UUID.randomUUID();
                    InventoryItem item = new InventoryItem();
                    item.setProduct(product);
                    item.setGame(product.getGame() != null ? product.getGame().getName() : "Genshin Impact");
                    item.setRankInfo("AR demo");
                    item.setSkinInfo("Random");
                    item.setExtraInfo("Acc seed tự động — đổi credentials trong admin.");
                    item.setCredentials(cred);
                    item.setCredentialHash(CredentialHasher.sha256(cred));
                    item.setListingImagePath(product.getImagePath());
                    item.setStatus(InventoryItemStatus.AVAILABLE);
                    item.setModerationStatus(ModerationStatus.APPROVED);
                    item.setHidden(false);
                    inventoryItemRepository.save(item);
                }
            }
        }

        if (accountRepository.countByRole(Role.ADMIN) == 0) {
            Account admin = new Account();
            admin.setUsername("admin");
            admin.setEmail("admin@localhost.local");
            admin.setPhone("0900000000");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setLocked(false);
            admin.setCreatedAt(new Date());
            accountRepository.save(admin);
        }
    }

    private Game getOrCreateGame(String slug, String name, String imagePath, String description) {
        return gameRepository.findBySlug(slug).orElseGet(() -> {
            Game g = new Game();
            g.setSlug(slug);
            g.setName(name);
            g.setImagePath(imagePath);
            g.setDescription(description);
            g.setVisible(true);
            g.setDisplayOrder(1);
            g.setCreatedAt(new Date());
            return gameRepository.save(g);
        });
    }

    private void seedProduct(String slug, String name, long price, String description, String imagePath, Game game) {
        Product p = new Product();
        p.setSlug(slug);
        p.setName(name);
        p.setPrice(price);
        p.setDescription(description);
        p.setImagePath(imagePath);
        p.setVisible(true);
        p.setGame(game);
        p.setCreatedAt(new Date());
        productRepository.save(p);
    }
}
