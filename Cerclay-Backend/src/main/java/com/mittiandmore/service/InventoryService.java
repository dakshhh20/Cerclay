package com.mittiandmore.service;

import com.mittiandmore.dto.InventoryAdjustmentResponse;
import com.mittiandmore.dto.InventoryAdjustmentRequest;
import com.mittiandmore.entity.InventoryAdjustment;
import com.mittiandmore.entity.Product;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.InventoryAdjustmentRepository;
import com.mittiandmore.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;

    public InventoryService(ProductRepository productRepository,
                            InventoryAdjustmentRepository adjustmentRepository) {
        this.productRepository = productRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Transactional
    public Product adjustStock(Long productId, InventoryAdjustmentRequest request, String actor) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        int oldStock = product.getStock() == null ? 0 : product.getStock();
        int newStock = request.getNewStock();

        if (newStock == oldStock) {
            return product;
        }

        product.setStock(newStock);
        Product saved = productRepository.save(product);

        recordStockChange(saved, oldStock, newStock, "ADMIN_ADJUSTMENT",
                request.getReason(), actor);

        return saved;
    }

    @Transactional
    public void restoreStockForCancellation(Long productId, int quantity, Long orderId) {
        if (quantity <= 0) {
            return;
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ApiException(
                        "PRODUCT_NOT_FOUND",
                        "Product not found while restoring cancelled-order stock",
                        HttpStatus.CONFLICT));

        int oldStock = product.getStock() == null ? 0 : product.getStock();
        int newStock = oldStock + quantity;
        product.setStock(newStock);
        Product saved = productRepository.save(product);

        recordStockChange(saved, oldStock, newStock, "ORDER_CANCELLED",
                "Stock restored for cancelled order #" + orderId, "SYSTEM");
    }

    @Transactional
    public void restoreStockForReturn(Long productId, int quantity, Long orderId) {
        if (quantity <= 0) return;
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found while restoring returned-order stock", HttpStatus.CONFLICT));
        int oldStock = product.getStock() == null ? 0 : product.getStock();
        int newStock = oldStock + quantity;
        product.setStock(newStock);
        Product saved = productRepository.save(product);
        recordStockChange(saved, oldStock, newStock, "RETURN_RECEIVED", "Stock restored for returned order #" + orderId, "SYSTEM");
    }

    @Transactional
    public void recordStockChange(Product product, int previousStock, int newStock,
                                  String changeType, String reason, String actor) {
        if (previousStock == newStock) return;

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(product);
        adjustment.setPreviousStock(previousStock);
        adjustment.setNewStock(newStock);
        adjustment.setChangeQuantity(newStock - previousStock);
        adjustment.setChangeType(changeType);
        adjustment.setReason(reason);
        adjustment.setActor(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        adjustmentRepository.save(adjustment);
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponse> recent() {
        return adjustmentRepository.findTop100ByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponse> forProduct(Long productId) {
        return adjustmentRepository.findTop100ByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toResponse).toList();
    }

    private InventoryAdjustmentResponse toResponse(InventoryAdjustment a) {
        InventoryAdjustmentResponse r = new InventoryAdjustmentResponse();
        r.setId(a.getId());
        r.setProductId(a.getProduct().getId());
        r.setProductName(a.getProduct().getName());
        r.setSku(a.getProduct().getSku());
        r.setPreviousStock(a.getPreviousStock());
        r.setNewStock(a.getNewStock());
        r.setChangeQuantity(a.getChangeQuantity());
        r.setChangeType(a.getChangeType());
        r.setReason(a.getReason());
        r.setActor(a.getActor());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
