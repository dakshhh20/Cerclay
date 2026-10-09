package com.mittiandmore.service;

import com.mittiandmore.dto.ProductImageRequest;
import com.mittiandmore.dto.ProductImageResponse;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.ProductImage;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.ProductImageRepository;
import com.mittiandmore.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public ProductImageService(ProductRepository productRepository, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(Long productId) {
        findProduct(productId);

        return productImageRepository
            .findByProductIdOrderByDisplayOrderAsc(productId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        Product product = findProduct(productId);

        if (request.getDisplayOrder() == null || request.getDisplayOrder() < 0) {
            throw new ApiException("INVALID_DISPLAY_ORDER", "Display order cannot be negative", HttpStatus.BAD_REQUEST);
        }

        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);

        boolean makePrimary = Boolean.TRUE.equals(request.getPrimary()) || existingImages.isEmpty();

        if (makePrimary) {
            existingImages.forEach(image -> image.setPrimary(false));
            productImageRepository.saveAll(existingImages);
        }

        ProductImage image = new ProductImage();

        image.setProduct(product);
        image.setImageUrl(request.getImageUrl());
        image.setPrimary(makePrimary);
        image.setDisplayOrder(request.getDisplayOrder());

        ProductImage saved = productImageRepository.save(image);
        if (Boolean.TRUE.equals(saved.getPrimary())) {
            product.setImage(saved.getImageUrl());
            productRepository.save(product);
        }
        return toResponse(saved);
    }

    @Transactional
    public ProductImageResponse setPrimary(Long productId, Long imageId) {
        findProduct(productId);

        ProductImage image = findImage(imageId);

        validateImageBelongsToProduct(image, productId);

        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);

        for (ProductImage productImage : images) {
            productImage.setPrimary(productImage.getId().equals(imageId));
        }

        productImageRepository.saveAll(images);
        Product product = image.getProduct();
        product.setImage(image.getImageUrl());
        productRepository.save(product);

        return toResponse(image);
    }

    @Transactional
    public ProductImageResponse updateImage(Long productId, Long imageId, ProductImageRequest request) {
        findProduct(productId);

        ProductImage image = findImage(imageId);

        validateImageBelongsToProduct(image, productId);

        if (request.getDisplayOrder() == null || request.getDisplayOrder() < 0) {
            throw new ApiException("INVALID_DISPLAY_ORDER", "Display order cannot be negative", HttpStatus.BAD_REQUEST);
        }

        image.setImageUrl(request.getImageUrl());
        image.setDisplayOrder(request.getDisplayOrder());

        if (Boolean.TRUE.equals(request.getPrimary())) {
            List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);

            for (ProductImage productImage : images) {
                productImage.setPrimary(productImage.getId().equals(imageId));
            }

            productImageRepository.saveAll(images);
        } else if (Boolean.FALSE.equals(request.getPrimary()) && Boolean.TRUE.equals(image.getPrimary())) {
            throw new ApiException(
                "INVALID_PRIMARY_IMAGE",
                "A product must have a primary image",
                HttpStatus.BAD_REQUEST
            );
        }

        ProductImage saved = productImageRepository.save(image);
        if (Boolean.TRUE.equals(saved.getPrimary())) {
            Product product = saved.getProduct();
            product.setImage(saved.getImageUrl());
            productRepository.save(product);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        findProduct(productId);

        ProductImage image = findImage(imageId);

        validateImageBelongsToProduct(image, productId);

        boolean wasPrimary = Boolean.TRUE.equals(image.getPrimary());

        productImageRepository.delete(image);

        if (wasPrimary) {
            List<ProductImage> remainingImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(
                productId
            );

            Product product = image.getProduct();
            if (!remainingImages.isEmpty()) {
                ProductImage newPrimary = remainingImages.get(0);
                newPrimary.setPrimary(true);

                productImageRepository.save(newPrimary);
                product.setImage(newPrimary.getImageUrl());
            } else {
                product.setImage(null);
            }
            productRepository.save(product);
        }
    }

    private Product findProduct(Long productId) {
        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));
    }

    private ProductImage findImage(Long imageId) {
        return productImageRepository
            .findById(imageId)
            .orElseThrow(() ->
                new ApiException("PRODUCT_IMAGE_NOT_FOUND", "Product image not found", HttpStatus.NOT_FOUND)
            );
    }

    private void validateImageBelongsToProduct(ProductImage image, Long productId) {
        if (
            image.getProduct() == null ||
            image.getProduct().getId() == null ||
            !image.getProduct().getId().equals(productId)
        ) {
            throw new ApiException(
                "PRODUCT_IMAGE_MISMATCH",
                "Product image does not belong to this product",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private ProductImageResponse toResponse(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();

        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setPrimary(image.getPrimary());
        response.setDisplayOrder(image.getDisplayOrder());

        return response;
    }
}
