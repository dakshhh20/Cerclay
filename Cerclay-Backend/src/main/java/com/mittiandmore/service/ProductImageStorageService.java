package com.mittiandmore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.mittiandmore.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private final Path root;
    private final String publicBaseUrl;

    public ProductImageStorageService(
            @Value("${app.uploads.product-dir:./uploads/products}") String directory,
            @Value("${app.uploads.public-base-url:/uploads/products}") String publicBaseUrl) throws IOException {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        migrateExistingUploads();
    }


    /**
     * Keep uploads independent of the IDE/terminal working directory. Older
     * Cerclay builds used ./uploads/products, so copy those existing files into
     * the stable upload directory when the backend starts. Existing files in
     * the stable directory are never overwritten.
     */
    private void migrateExistingUploads() {
        try {
            Path working = Paths.get(System.getProperty("user.dir"))
                    .toAbsolutePath().normalize();
            Set<Path> candidates = new java.util.LinkedHashSet<>();
            candidates.add(working.resolve("uploads/products"));
            Path current = working;
            for (int i = 0; i < 4 && current != null; i++) {
                candidates.add(current.resolve("uploads/products"));
                candidates.add(current.resolve("Cerclay-Backend/uploads/products"));
                Path parent = current.getParent();
                if (parent != null) {
                    try (var stream = Files.list(parent)) {
                        stream.filter(Files::isDirectory)
                                .filter(p -> p.getFileName().toString().toLowerCase().contains("backend"))
                                .forEach(p -> candidates.add(p.resolve("uploads/products")));
                    } catch (Exception ignored) { }
                }
                current = current.getParent();
            }

            for (Path source : candidates) {
                Path normalized = source.toAbsolutePath().normalize();
                if (normalized.equals(root) || !Files.isDirectory(normalized)) continue;
                try (var files = Files.list(normalized)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        try {
                            Path target = root.resolve(file.getFileName().toString()).normalize();
                            if (target.getParent().equals(root) && !Files.exists(target)) {
                                Files.copy(file, target);
                            }
                        } catch (Exception ignored) { }
                    });
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) {
            // Upload serving must never prevent the application from starting.
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException("IMAGE_REQUIRED","Image file is required",HttpStatus.BAD_REQUEST);
        if (file.getSize() > MAX_BYTES) throw new ApiException("IMAGE_TOO_LARGE","Image must be 5 MB or smaller",HttpStatus.BAD_REQUEST);
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) throw new ApiException("INVALID_IMAGE_TYPE","Only JPG, PNG and WebP images are allowed",HttpStatus.BAD_REQUEST);
        String extension = extension(file.getOriginalFilename(), type);
        String filename = UUID.randomUUID() + extension;
        Path target = root.resolve(filename).normalize();
        if (!target.getParent().equals(root)) throw new ApiException("INVALID_IMAGE_PATH","Invalid image path",HttpStatus.BAD_REQUEST);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store product image", e);
        }
        return publicBaseUrl + "/" + filename;
    }

    private String extension(String original, String contentType) {
        String ext = StringUtils.getFilenameExtension(original);
        if (ext != null && Set.of("jpg","jpeg","png","webp").contains(ext.toLowerCase())) return "." + ext.toLowerCase().replace("jpeg","jpg");
        return switch (contentType) { case "image/png" -> ".png"; case "image/webp" -> ".webp"; default -> ".jpg"; };
    }
}
