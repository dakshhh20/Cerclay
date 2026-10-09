package com.mittiandmore.service;

import com.mittiandmore.dto.CartItemResponse;
import com.mittiandmore.dto.CartProductResponse;
import com.mittiandmore.dto.CartResponse;
import com.mittiandmore.dto.ProductImageResponse;
import com.mittiandmore.entity.Cart;
import com.mittiandmore.entity.CartItem;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Product;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.CartRepository;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CartService(
        CartRepository cartRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartResponse getOrCreateGuestCart(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new ApiException("GUEST_ID_REQUIRED", "Guest ID is required", HttpStatus.BAD_REQUEST);
        }

        Cart cart = cartRepository.findByGuestId(guestId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setGuestId(guestId);
            return cartRepository.save(newCart);
        });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse getOrCreateCustomerCart(Long customerId) {
        Customer customer = customerRepository
            .findById(customerId)
            .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));

        Cart cart = cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setCustomer(customer);
            return cartRepository.save(newCart);
        });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCustomerCart(Long customerId, Long productId, int quantity, int packSize) {
        Cart cart = getOrCreateCustomerCartEntity(customerId);

        addProductToCart(cart, productId, quantity, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateCustomerCartItemQuantity(Long customerId, Long productId, int quantity, int packSize) {
        Cart cart = getOrCreateCustomerCartEntity(customerId);

        updateCartItemQuantity(cart, productId, quantity, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeCustomerCartItem(Long customerId, Long productId, int packSize) {
        Cart cart = getOrCreateCustomerCartEntity(customerId);

        removeCartItem(cart, productId, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse addItem(String guestId, Long productId, int quantity, int packSize) {
        Cart cart = getOrCreateGuestCartEntity(guestId);

        addProductToCart(cart, productId, quantity, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(String guestId, Long productId, int quantity, int packSize) {
        Cart cart = getOrCreateGuestCartEntity(guestId);

        updateCartItemQuantity(cart, productId, quantity, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String guestId, Long productId, int packSize) {
        Cart cart = getOrCreateGuestCartEntity(guestId);

        removeCartItem(cart, productId, packSize);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse mergeGuestCart(Long customerId, String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return getOrCreateCustomerCart(customerId);
        }

        Cart customerCart = getOrCreateCustomerCartEntity(customerId);
        Cart guestCart = cartRepository.findByGuestId(guestId).orElse(null);

        if (guestCart == null || guestCart.getItems().isEmpty()) {
            return toResponse(customerCart);
        }

        for (CartItem guestItem : new java.util.ArrayList<>(guestCart.getItems())) {
            Product guestProduct = guestItem.getProduct();
            if (guestProduct == null) {
                continue;
            }

            boolean merged = false;
            for (CartItem customerItem : customerCart.getItems()) {
                if (
                    customerItem.getProduct() != null &&
                    customerItem.getProduct().getId().equals(guestProduct.getId()) &&
                    customerItem.getPackSize() == guestItem.getPackSize()
                ) {
                    int mergedQuantity = customerItem.getQuantity() + guestItem.getQuantity();
                    int allowedQuantity = Math.max(0, guestProduct.getStock());
                    customerItem.setQuantity(Math.min(mergedQuantity, allowedQuantity));
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                CartItem newItem = new CartItem();
                newItem.setCart(customerCart);
                newItem.setProduct(guestProduct);
                newItem.setQuantity(
                    Math.min(
                        guestItem.getQuantity(),
                        Math.max(0, guestProduct.getStock() / Math.max(1, guestItem.getPackSize()))
                    )
                );
                newItem.setPackSize(guestItem.getPackSize());
                if (newItem.getQuantity() > 0) {
                    customerCart.getItems().add(newItem);
                }
            }
        }

        guestCart.getItems().clear();
        cartRepository.delete(guestCart);

        return toResponse(cartRepository.save(customerCart));
    }

    private Cart getOrCreateGuestCartEntity(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new ApiException("GUEST_ID_REQUIRED", "Guest ID is required", HttpStatus.BAD_REQUEST);
        }
        return cartRepository.findByGuestId(guestId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setGuestId(guestId);
            return cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCustomerCartEntity(Long customerId) {
        Customer customer = customerRepository
            .findById(customerId)
            .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomer(customer);
            return cartRepository.save(cart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setGuestId(cart.getGuestId());
        response.setItems(
            cart
                .getItems()
                .stream()
                .map(item -> {
                    CartItemResponse itemResponse = new CartItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setPackSize(item.getPackSize());

                    Product product = item.getProduct();
                    CartProductResponse productResponse = new CartProductResponse();
                    productResponse.setId(product.getId());
                    productResponse.setName(product.getName());
                    productResponse.setCategory(product.getCategory());
                    productResponse.setSku(product.getSku());
                    productResponse.setSlug(product.getSlug());
                    productResponse.setPrice(product.getPrice());
                    productResponse.setMrp(product.getMrp());
                    productResponse.setStock(product.getStock());
                    productResponse.setActive(product.getActive());
                    productResponse.setSetOf2Enabled(product.getSetOf2Enabled());
                    productResponse.setSetOf2Price(product.getSetOf2Price());

                    List<ProductImageResponse> images = product
                        .getImages()
                        .stream()
                        .map(image -> {
                            ProductImageResponse imageResponse = new ProductImageResponse();
                            imageResponse.setId(image.getId());
                            imageResponse.setImageUrl(image.getImageUrl());
                            imageResponse.setPrimary(image.getPrimary());
                            imageResponse.setDisplayOrder(image.getDisplayOrder());
                            return imageResponse;
                        })
                        .toList();
                    productResponse.setImages(images);

                    String primary = images
                        .stream()
                        .filter(image -> Boolean.TRUE.equals(image.getPrimary()))
                        .map(ProductImageResponse::getImageUrl)
                        .findFirst()
                        .orElse(product.getImage());
                    productResponse.setImage(primary);

                    itemResponse.setProduct(productResponse);
                    return itemResponse;
                })
                .toList()
        );
        return response;
    }

    private void addProductToCart(Cart cart, Long productId, int quantity, int packSize) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        validateProduct(product);

        validatePack(product, packSize);

        if (quantity <= 0) {
            throw new ApiException("INVALID_QUANTITY", "Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        int unitsRequested = quantity * packSize;
        if (product.getStock() < unitsRequested) {
            throw new ApiException("INSUFFICIENT_STOCK", "Not enough stock", HttpStatus.BAD_REQUEST);
        }

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId) && item.getPackSize() == packSize) {
                int newQuantity = item.getQuantity() + quantity;

                if (newQuantity * packSize > product.getStock()) {
                    throw new ApiException("INSUFFICIENT_STOCK", "Not enough stock", HttpStatus.BAD_REQUEST);
                }

                item.setQuantity(newQuantity);
                return;
            }
        }

        CartItem newItem = new CartItem();

        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);
        newItem.setPackSize(packSize);

        cart.getItems().add(newItem);
    }

    private void updateCartItemQuantity(Cart cart, Long productId, int quantity, int packSize) {
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        validateProduct(product);

        validatePack(product, packSize);

        if (quantity <= 0) {
            throw new ApiException("INVALID_QUANTITY", "Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        if (quantity * packSize > product.getStock()) {
            throw new ApiException("INSUFFICIENT_STOCK", "Not enough stock", HttpStatus.BAD_REQUEST);
        }

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId) && item.getPackSize() == packSize) {
                item.setQuantity(quantity);
                return;
            }
        }

        throw new ApiException("PRODUCT_NOT_IN_CART", "Product is not in cart", HttpStatus.NOT_FOUND);
    }

    private void removeCartItem(Cart cart, Long productId, int packSize) {
        CartItem itemToRemove = null;

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId) && item.getPackSize() == packSize) {
                itemToRemove = item;
                break;
            }
        }

        if (itemToRemove == null) {
            throw new ApiException("PRODUCT_NOT_IN_CART", "Product is not in cart", HttpStatus.NOT_FOUND);
        }

        cart.getItems().remove(itemToRemove);
    }

    private void validatePack(Product product, int packSize) {
        if (packSize != 1 && packSize != 2) throw new ApiException(
            "INVALID_PACK_SIZE",
            "Pack size must be 1 or 2",
            HttpStatus.BAD_REQUEST
        );
        if (packSize == 2 && (!Boolean.TRUE.equals(product.getSetOf2Enabled()) || product.getSetOf2Price() == null)) {
            throw new ApiException(
                "SET_OF_2_NOT_AVAILABLE",
                "Set of 2 is not available for this product",
                HttpStatus.BAD_REQUEST
            );
        }
        if (packSize == 2 && product.getSetOf2Price().signum() < 0) throw new ApiException(
            "INVALID_PACK_PRICE",
            "Set of 2 price is invalid",
            HttpStatus.BAD_REQUEST
        );
    }

    private void validateProduct(Product product) {
        if (!product.getActive()) {
            throw new ApiException("PRODUCT_NOT_AVAILABLE", "Product is not available", HttpStatus.BAD_REQUEST);
        }
    }
}
