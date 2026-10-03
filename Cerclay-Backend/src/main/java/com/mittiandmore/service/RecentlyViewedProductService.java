package com.mittiandmore.service;

import com.mittiandmore.dto.ProductResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.RecentlyViewedProduct;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.ProductRepository;
import com.mittiandmore.repository.RecentlyViewedProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecentlyViewedProductService {

    private final RecentlyViewedProductRepository recentlyViewedRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public RecentlyViewedProductService(
            RecentlyViewedProductRepository recentlyViewedRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ProductService productService) {
        this.recentlyViewedRepository = recentlyViewedRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRecentlyViewed(Long customerId) {
        return recentlyViewedRepository
                .findTop12ByCustomerIdAndProductActiveTrueOrderByLastViewedAtDesc(customerId)
                .stream()
                .map(item -> productService.getProductById(item.getProduct().getId()))
                .toList();
    }

    @Transactional
    public List<ProductResponse> recordView(Long customerId, Long productId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException(
                        "CUSTOMER_NOT_FOUND",
                        "Customer account not found",
                        HttpStatus.NOT_FOUND
                ));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(
                        "PRODUCT_NOT_FOUND",
                        "Product not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!Boolean.TRUE.equals(product.getActive())) {
            return getRecentlyViewed(customerId);
        }

        RecentlyViewedProduct item = recentlyViewedRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .orElseGet(RecentlyViewedProduct::new);

        item.setCustomer(customer);
        item.setProduct(product);
        item.setLastViewedAt(LocalDateTime.now());
        recentlyViewedRepository.save(item);

        return getRecentlyViewed(customerId);
    }
}
