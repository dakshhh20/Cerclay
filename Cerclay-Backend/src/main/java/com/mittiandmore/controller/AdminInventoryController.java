package com.mittiandmore.controller;

import com.mittiandmore.dto.InventoryAdjustmentRequest;
import com.mittiandmore.dto.InventoryAdjustmentResponse;
import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.service.InventoryService;
import com.mittiandmore.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;

    public AdminInventoryController(InventoryService inventoryService, ProductService productService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> adjustStock(
        @PathVariable Long productId,
        @Valid @RequestBody InventoryAdjustmentRequest request,
        Authentication authentication
    ) {
        var product = inventoryService.adjustStock(
            productId,
            request,
            authentication == null ? "ADMIN" : authentication.getName()
        );
        return ResponseEntity.ok(productService.getProductById(product.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InventoryAdjustmentResponse>> history() {
        return ResponseEntity.ok(inventoryService.recent());
    }

    @GetMapping("/{productId}/history")
    public ResponseEntity<List<InventoryAdjustmentResponse>> productHistory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.forProduct(productId));
    }
}
