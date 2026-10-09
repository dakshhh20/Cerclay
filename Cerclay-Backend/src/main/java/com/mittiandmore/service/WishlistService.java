package com.mittiandmore.service;

import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.WishlistItem;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.ProductRepository;
import com.mittiandmore.repository.WishlistItemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public WishlistService(
        WishlistItemRepository wishlistItemRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        ProductService productService
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist(Long customerId) {
        return wishlistItemRepository
            .findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .map(item -> productService.getProductById(item.getProduct().getId()))
            .toList();
    }

    @Transactional
    public List<ProductResponse> add(Long customerId, Long productId) {
        Customer customer = customerRepository
            .findById(customerId)
            .orElseThrow(() ->
                new ApiException("CUSTOMER_NOT_FOUND", "Customer account not found", HttpStatus.NOT_FOUND)
            );

        Product product = productRepository
            .findById(productId)
            .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ApiException("PRODUCT_UNAVAILABLE", "This product is no longer available", HttpStatus.CONFLICT);
        }

        if (!wishlistItemRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            WishlistItem item = new WishlistItem();
            item.setCustomer(customer);
            item.setProduct(product);
            wishlistItemRepository.save(item);
        }

        return getWishlist(customerId);
    }

    @Transactional
    public List<ProductResponse> remove(Long customerId, Long productId) {
        wishlistItemRepository
            .findByCustomerIdAndProductId(customerId, productId)
            .ifPresent(wishlistItemRepository::delete);
        return getWishlist(customerId);
    }
}
