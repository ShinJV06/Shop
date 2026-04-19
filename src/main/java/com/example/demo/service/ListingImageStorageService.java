package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ListingImageStorageService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path root = Paths.get("uploads", "listings").toAbsolutePath().normalize();

    /**
     * Lưu ảnh vào thư mục uploads/listings, trả về URL tương đối (vd: /listings/uuid.png).
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Thiếu file ảnh.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Ảnh tối đa 5MB.");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
        }
        String ext = extensionFromContentType(ct);
        String name = UUID.randomUUID() + ext;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(name);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh: " + e.getMessage(), e);
        }
        return "/listings/" + name;
    }

    private static String extensionFromContentType(String contentType) {
        String c = contentType.split(";")[0].trim().toLowerCase();
        if (c.equals("image/jpeg") || c.equals("image/jpg")) {
            return ".jpg";
        }
        if (c.equals("image/png")) {
            return ".png";
        }
        if (c.equals("image/webp")) {
            return ".webp";
        }
        if (c.equals("image/gif")) {
            return ".gif";
        }
        throw new IllegalArgumentException("Định dạng ảnh không hỗ trợ (JPEG, PNG, GIF, WebP).");
    }
}
