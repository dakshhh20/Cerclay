package com.mittiandmore.controller;

import com.mittiandmore.dto.ProductImageRequest;
import com.mittiandmore.dto.ProductImageResponse;
import com.mittiandmore.service.ProductImageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/images")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> getProductImages(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getProductImages(productId));
    }

    @PostMapping
    public ResponseEntity<ProductImageResponse> addImage(
        @PathVariable Long productId,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.addImage(productId, request));
    }

    @PutMapping("/{imageId}")
    public ResponseEntity<ProductImageResponse> updateImage(
        @PathVariable Long productId,
        @PathVariable Long imageId,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return ResponseEntity.ok(productImageService.updateImage(productId, imageId, request));
    }

    @PutMapping("/{imageId}/primary")
    public ResponseEntity<ProductImageResponse> setPrimary(@PathVariable Long productId, @PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.setPrimary(productId, imageId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long productId, @PathVariable Long imageId) {
        productImageService.deleteImage(productId, imageId);

        return ResponseEntity.noContent().build();
    }
}
