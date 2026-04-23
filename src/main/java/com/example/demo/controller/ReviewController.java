package com.example.demo.controller;

import com.example.demo.entity.Review;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Optional;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AccountRepository accountRepository;

    private static final String UPLOAD_DIR = "uploads/reviews/";

    @GetMapping
    public String listReviews(Model model, HttpSession session) {
        // Chỉ hiển thị review đã được duyệt
        List<Review> reviews = reviewRepository.findByApprovedTrueOrderByCreatedAtDesc();

        List<Map<String, Object>> reviewsWithDate = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Review r : reviews) {
            Map<String, Object> m = new HashMap<>();
            m.put("review", r);
            m.put("formattedDate", r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "");
            reviewsWithDate.add(m);
        }

        model.addAttribute("reviewsWithDate", reviewsWithDate);

        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average().orElse(0);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", reviews.size());

        // Kiểm tra user đã đăng nhập chưa
        boolean isLoggedIn = session.getAttribute("username") != null;
        model.addAttribute("isLoggedIn", isLoggedIn);
        
        if (isLoggedIn) {
            String username = (String) session.getAttribute("username");
            model.addAttribute("currentUsername", username);
        }

        // Đếm review chờ duyệt
        long pendingCount = reviewRepository.countByApprovedFalse();
        model.addAttribute("pendingCount", pendingCount);

        return "reviews";
    }

    @GetMapping("/product/{slug}")
    public String productReviews(@PathVariable String slug, Model model, HttpSession session) {
        List<Review> reviews = reviewRepository.findByProductSlugAndApprovedTrueOrderByCreatedAtDesc(slug);
        model.addAttribute("reviews", reviews);
        model.addAttribute("productSlug", slug);

        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average().orElse(0);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalReviews", reviews.size());

        boolean isLoggedIn = session.getAttribute("username") != null;
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "reviews";
    }

    @PostMapping("/submit")
    public String submitReview(
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String productSlug,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) List<MultipartFile> images,
            HttpSession session,
            Model model
    ) {
        // Kiểm tra đăng nhập - YÊU CẦU PHẢI ĐĂNG NHẬP
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/auth/login?redirect=/reviews";
        }

        Review review = new Review();
        review.setUsername(username);
        review.setUserAvatar(null);
        review.setRating(rating);
        review.setComment(comment);
        review.setProductSlug(productSlug);
        review.setProductName(productName);
        review.setCreatedAt(LocalDateTime.now());
        
        // MẶC ĐỊNH LÀ CHƯA DUYỆT - CẦN ADMIN DUYỆT MỚI HIỂN THỊ
        review.setApproved(false);
        review.setModerationStatus("PENDING");

        if (images != null && !images.isEmpty()) {
            List<String> imagePaths = new ArrayList<>();
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
                        Path filePath = uploadPath.resolve(filename);
                        image.transferTo(filePath.toFile());
                        imagePaths.add("/" + UPLOAD_DIR + filename);
                    }
                }
                review.setImagePaths(String.join(",", imagePaths));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        reviewRepository.save(review);

        // Thông báo chờ duyệt
        if (productSlug != null && !productSlug.isBlank()) {
            return "redirect:/reviews/product/" + productSlug + "?pending=true";
        }
        return "redirect:/reviews?pending=true";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReviewsApi(HttpSession session) {
        // Chỉ trả về review đã duyệt
        List<Review> reviews = reviewRepository.findByApprovedTrueOrderByCreatedAtDesc();
        Map<String, Object> result = new HashMap<>();

        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average().orElse(0);

        result.put("reviews", reviews);
        result.put("avgRating", avgRating);
        result.put("totalReviews", reviews.size());

        return ResponseEntity.ok(result);
    }

    // ========== ADMIN MODERATION ENDPOINTS ==========

    @GetMapping("/admin/pending")
    public String pendingReviews(Model model, HttpSession session) {
        // Kiểm tra quyền admin
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/auth/login";
        }

        List<Review> pendingReviews = reviewRepository.findByApprovedFalseOrderByCreatedAtDesc();
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("pendingCount", pendingReviews.size());
        
        return "admin/reviews-pending";
    }

    @PostMapping("/admin/approve/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveReview(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Không có quyền"));
        }

        Optional<Review> optReview = reviewRepository.findById(id);
        if (optReview.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Review không tồn tại"));
        }

        Review review = optReview.get();
        review.setApproved(true);
        review.setModerationStatus("APPROVED");
        reviewRepository.save(review);

        return ResponseEntity.ok(Map.of("success", true, "message", "Đã duyệt review"));
    }

    @PostMapping("/admin/reject/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectReview(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Không có quyền"));
        }

        Optional<Review> optReview = reviewRepository.findById(id);
        if (optReview.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Review không tồn tại"));
        }

        Review review = optReview.get();
        review.setApproved(false);
        review.setModerationStatus("REJECTED");
        reviewRepository.save(review);

        return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối review"));
    }

    @PostMapping("/admin/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Không có quyền"));
        }

        if (!reviewRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Review không tồn tại"));
        }

        reviewRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa review"));
    }

    @GetMapping("/admin/all")
    public String allReviews(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return "redirect:/auth/login";
        }

        List<Review> allReviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("allReviews", allReviews);
        
        long approvedCount = reviewRepository.countByApprovedTrue();
        long pendingCount = reviewRepository.countByApprovedFalse();
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("pendingCount", pendingCount);
        
        return "admin/reviews-all";
    }
}
