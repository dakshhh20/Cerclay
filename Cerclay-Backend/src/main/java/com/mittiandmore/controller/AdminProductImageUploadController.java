package com.mittiandmore.controller;

import com.mittiandmore.dto.ProductImageRequest;
import com.mittiandmore.dto.ProductImageResponse;
import com.mittiandmore.service.ProductImageService;
import com.mittiandmore.service.ProductImageStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products/{productId}/images")
public class AdminProductImageUploadController {

    private final ProductImageStorageService storageService;
    private final ProductImageService imageService;

    public AdminProductImageUploadController(
        ProductImageStorageService storageService,
        ProductImageService imageService
    ) {
        this.storageService = storageService;
        this.imageService = imageService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ProductImageResponse> upload(
        @PathVariable Long productId,
        @RequestPart("file") MultipartFile file
    ) {
        String url = storageService.store(file);
        ProductImageRequest request = new ProductImageRequest();
        request.setImageUrl(url);
        request.setPrimary(false);
        request.setDisplayOrder(imageService.getProductImages(productId).size());
        return ResponseEntity.ok(imageService.addImage(productId, request));
    }
}
