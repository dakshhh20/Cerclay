package com.mittiandmore.service;

import com.mittiandmore.dto.ProductImageResponse;
import com.mittiandmore.dto.ProductRequest;
import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.ProductImage;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.ProductRepository;
import com.mittiandmore.repository.CartItemRepository;
import com.mittiandmore.repository.InventoryAdjustmentRepository;
import com.mittiandmore.repository.OrderItemRepository;
import com.mittiandmore.repository.ProductReviewRepository;
import com.mittiandmore.repository.RecentlyViewedProductRepository;
import com.mittiandmore.repository.WishlistItemRepository;
import com.mittiandmore.specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final CartItemRepository cartItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository productReviewRepository;
    private final RecentlyViewedProductRepository recentlyViewedProductRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public ProductService(ProductRepository productRepository,
                          InventoryService inventoryService,
                          CartItemRepository cartItemRepository,
                          InventoryAdjustmentRepository inventoryAdjustmentRepository,
                          OrderItemRepository orderItemRepository,
                          ProductReviewRepository productReviewRepository,
                          RecentlyViewedProductRepository recentlyViewedProductRepository,
                          WishlistItemRepository wishlistItemRepository) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.cartItemRepository = cartItemRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.orderItemRepository = orderItemRepository;
        this.productReviewRepository = productReviewRepository;
        this.recentlyViewedProductRepository = recentlyViewedProductRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> searchProducts(
            String search,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            String sort,
            String colorGroup
    ) {

        Specification<Product> specification =
                ProductSpecification.isActive();

        if (search != null && !search.isBlank()) {
            specification = specification.and(
                    ProductSpecification.hasSearch(search)
            );
        }

        if (category != null && !category.isBlank()) {
            specification = specification.and(
                    ProductSpecification.hasCategory(category)
            );
        }

        if (minPrice != null) {
            specification = specification.and(
                    ProductSpecification.hasMinPrice(minPrice)
            );
        }

        if (maxPrice != null) {
            specification = specification.and(
                    ProductSpecification.hasMaxPrice(maxPrice)
            );
        }

        if (Boolean.TRUE.equals(inStock)) {
            specification = specification.and(
                    ProductSpecification.hasStock()
            );
        }

        if (colorGroup != null && !colorGroup.isBlank()) {
            specification = specification.and(
                    ProductSpecification.hasColorGroup(colorGroup)
            );
        }

        Sort ordering = switch (sort == null ? "" : sort.trim().toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "featured" -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        return productRepository
                .findAll(specification, ordering)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {

        Product product = findProduct(id);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        validateProduct(request);

        if (productRepository.existsBySku(request.getSku())) {
            throw new ApiException(
                    "DUPLICATE_SKU",
                    "A product with this SKU already exists",
                    HttpStatus.CONFLICT
            );
        }

        if (productRepository.existsBySlug(request.getSlug())) {
            throw new ApiException(
                    "DUPLICATE_SLUG",
                    "A product with this slug already exists",
                    HttpStatus.CONFLICT
            );
        }

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setSku(request.getSku());
        product.setSlug(request.getSlug());
        product.setPrice(request.getPrice());
        product.setMrp(request.getMrp());
        product.setStock(request.getStock());

        product.setActive(
                request.getActive() == null || request.getActive()
        );
        product.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        product.setSetOf2Enabled(Boolean.TRUE.equals(request.getSetOf2Enabled()));
        product.setSetOf2Price(request.getSetOf2Price());
        product.setSetOf2Mrp(request.getSetOf2Mrp());
        product.setColorGroup(clean(request.getColorGroup()));
        product.setColorName(clean(request.getColorName()));
        product.setColorHex(normaliseHex(request.getColorHex()));

        product.setReviews(0);
        product.setRating(null);

        Product savedProduct =
                productRepository.save(product);

        if (savedProduct.getStock() != null && savedProduct.getStock() > 0) {
            inventoryService.recordStockChange(
                    savedProduct, 0, savedProduct.getStock(),
                    "OPENING_STOCK", "Opening stock", "ADMIN"
            );
        }

        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product existingProduct = findProduct(id);

        validateProduct(request);

        if (!existingProduct.getSku().equals(request.getSku())
                && productRepository.existsBySku(request.getSku())) {

            throw new ApiException(
                    "DUPLICATE_SKU",
                    "A product with this SKU already exists",
                    HttpStatus.CONFLICT
            );
        }

        if (!existingProduct.getSlug().equals(request.getSlug())
                && productRepository.existsBySlug(request.getSlug())) {

            throw new ApiException(
                    "DUPLICATE_SLUG",
                    "A product with this slug already exists",
                    HttpStatus.CONFLICT
            );
        }

        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setCategory(request.getCategory());
        existingProduct.setSku(request.getSku());
        existingProduct.setSlug(request.getSlug());
        existingProduct.setPrice(request.getPrice());
        int previousStock = existingProduct.getStock();

        existingProduct.setMrp(request.getMrp());
        existingProduct.setStock(request.getStock());

        if (request.getActive() != null) {
            existingProduct.setActive(request.getActive());
        }
        existingProduct.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        existingProduct.setSetOf2Enabled(Boolean.TRUE.equals(request.getSetOf2Enabled()));
        existingProduct.setSetOf2Price(request.getSetOf2Price());
        existingProduct.setSetOf2Mrp(request.getSetOf2Mrp());
        existingProduct.setColorGroup(clean(request.getColorGroup()));
        existingProduct.setColorName(clean(request.getColorName()));
        existingProduct.setColorHex(normaliseHex(request.getColorHex()));

        Product savedProduct =
                productRepository.save(existingProduct);

        inventoryService.recordStockChange(
                savedProduct, previousStock, savedProduct.getStock(),
                "PRODUCT_UPDATE", "Stock changed from product editor", "ADMIN"
        );

        return toResponse(savedProduct);
    }


    /**
     * Removes a product safely from the catalogue. Products that have already
     * been used in an order are archived (deactivated) so historical orders
     * keep their product relationship intact. Products with no order history
     * can be permanently deleted after clearing non-historical references.
     */
    @Transactional
    public ProductResponse removeProduct(Long id) {
        Product product = findProduct(id);

        if (orderItemRepository.countByProductId(id) > 0) {
            // Keep the product row for historical order integrity, but remove it
            // from the normal catalogue by archiving it.
            product.setActive(false);
            product.setArchived(true);
            return toResponse(productRepository.save(product));
        }

        // Unused products can be permanently removed. Clear all non-historical
        // references first so old/test products cannot get stuck behind FK constraints.
        cartItemRepository.deleteByProductId(id);
        wishlistItemRepository.deleteByProductId(id);
        recentlyViewedProductRepository.deleteByProductId(id);
        productReviewRepository.deleteByProductId(id);
        inventoryAdjustmentRepository.deleteByProductId(id);
        productRepository.delete(product);
        productRepository.flush();
        return null;
    }

    @Transactional
    public ProductResponse deactivateProduct(Long id) {

        Product product = findProduct(id);

        product.setActive(false);

        return toResponse(
                productRepository.save(product)
        );
    }

    @Transactional
    public ProductResponse activateProduct(Long id) {

        Product product = findProduct(id);

        product.setActive(true);
        product.setArchived(false);

        return toResponse(
                productRepository.save(product)
        );
    }

    public boolean isStockAvailable(
            Long id,
            int quantity
    ) {

        Product product = findProduct(id);

        if (!product.getActive()) {
            throw new ApiException(
                    "PRODUCT_INACTIVE",
                    "Product is not available",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (quantity <= 0) {
            throw new ApiException(
                    "INVALID_QUANTITY",
                    "Quantity must be greater than 0",
                    HttpStatus.BAD_REQUEST
            );
        }

        return product.getStock() >= quantity;
    }

    private Product findProduct(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        "PRODUCT_NOT_FOUND",
                        "Product not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    private void validateProduct(ProductRequest request) {

        if (request.getPrice().compareTo(request.getMrp()) > 0) {
            throw new ApiException(
                    "INVALID_PRODUCT",
                    "Product price cannot be greater than MRP",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (request.getStock() < 0) {
            throw new ApiException(
                    "INVALID_PRODUCT",
                    "Product stock cannot be negative",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (Boolean.TRUE.equals(request.getSetOf2Enabled())) {
            if (request.getSetOf2Price() == null || request.getSetOf2Price().signum() < 0) {
                throw new ApiException("INVALID_PRODUCT", "Set of 2 price is required when the bundle is enabled", HttpStatus.BAD_REQUEST);
            }
            if (request.getSetOf2Price().compareTo(request.getPrice().multiply(BigDecimal.valueOf(2))) > 0) {
                throw new ApiException("INVALID_PRODUCT", "Set of 2 price cannot be greater than two times the selling price", HttpStatus.BAD_REQUEST);
            }
            if (request.getSetOf2Mrp() != null) {
                if (request.getSetOf2Price().compareTo(request.getSetOf2Mrp()) > 0) {
                    throw new ApiException("INVALID_PRODUCT", "Set of 2 price cannot be greater than its MRP", HttpStatus.BAD_REQUEST);
                }
                if (request.getSetOf2Mrp().compareTo(request.getMrp().multiply(BigDecimal.valueOf(2))) > 0) {
                    throw new ApiException("INVALID_PRODUCT", "Set of 2 MRP cannot be greater than two times the single MRP", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normaliseHex(String value) {
        String cleaned = clean(value);
        if (cleaned == null) return null;
        if (!cleaned.matches("#[0-9a-fA-F]{6}")) {
            throw new ApiException("INVALID_PRODUCT", "Color swatch must be a 6-digit hex color such as #C45A4A", HttpStatus.BAD_REQUEST);
        }
        return cleaned.toUpperCase();
    }

    private ProductResponse toResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setSku(product.getSku());
        response.setSlug(product.getSlug());
        response.setPrice(product.getPrice());
        response.setMrp(product.getMrp());
        response.setStock(product.getStock());
        response.setActive(product.getActive());
        response.setArchived(product.getArchived());
        response.setFeatured(product.getFeatured());
        response.setSetOf2Enabled(product.getSetOf2Enabled());
        response.setSetOf2Price(product.getSetOf2Price());
        response.setSetOf2Mrp(product.getSetOf2Mrp());
        response.setColorGroup(product.getColorGroup());
        response.setColorName(product.getColorName());
        response.setColorHex(product.getColorHex());
        response.setRating(product.getRating());
        response.setReviews(product.getReviews());
        List<ProductImageResponse> imageResponses =
                product.getImages()
                        .stream()
                        .map(this::toImageResponse)
                        .toList();

        // Product.image is a legacy field. Prefer the actual primary ProductImage
        // so replacing/deleting images in the admin cannot leave the storefront
        // pointing at a stale/broken legacy URL.
        String primaryImageUrl = product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImage());
        response.setImage(primaryImageUrl);
        response.setImages(imageResponses);

        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        return response;
    }

    private ProductImageResponse toImageResponse(
            ProductImage image
    ) {

        ProductImageResponse response =
                new ProductImageResponse();

        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setPrimary(image.getPrimary());
        response.setDisplayOrder(image.getDisplayOrder());

        return response;
    }
}