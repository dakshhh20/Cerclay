package com.mittiandmore.service;

import com.mittiandmore.dto.ProductReviewRequest;
import com.mittiandmore.dto.ProductReviewResponse;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.ProductReview;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.ProductRepository;
import com.mittiandmore.repository.ProductReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    public ProductReviewService(ProductReviewRepository r, ProductRepository p, CustomerRepository c){reviewRepository=r;productRepository=p;customerRepository=c;}

    public List<ProductReviewResponse> approved(Long productId){
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId,"APPROVED").stream().map(this::toResponse).toList();
    }
    public List<ProductReviewResponse> all(){return reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();}
    public List<ProductReviewResponse> mine(Long customerId){return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(this::toResponse).toList();}
    public List<ProductReviewResponse> featured(){return reviewRepository.findTop6ByStatusOrderByCreatedAtDesc("APPROVED").stream().map(this::toResponse).toList();}
    public boolean eligible(Long customerId, Long productId){
        if (reviewRepository.findByCustomerIdAndProductId(customerId,productId).isPresent()) return false;
        return reviewRepository.hasDeliveredPurchase(customerId,productId);
    }
    @Transactional
    public ProductReviewResponse create(Long customerId, Long productId, ProductReviewRequest request){
        if(!eligible(customerId,productId)) throw new IllegalStateException("You can review this product only after a delivered purchase, and only once.");
        Product p=productRepository.findById(productId).orElseThrow(()->new IllegalArgumentException("Product not found."));
        Customer c=customerRepository.findById(customerId).orElseThrow(()->new IllegalStateException("Customer not found."));
        ProductReview r=new ProductReview(); r.setProduct(p); r.setCustomer(c); r.setRating(request.getRating()); r.setReview(request.getReview().trim()); r.setStatus("PENDING"); r.setVerifiedPurchase(true);
        return toResponse(reviewRepository.save(r));
    }
    @Transactional
    public ProductReviewResponse moderate(Long id, String status){
        String normalized=status==null?"":status.trim().toUpperCase();
        if(!List.of("APPROVED","REJECTED","PENDING").contains(normalized)) throw new IllegalArgumentException("Invalid review status.");
        ProductReview r=reviewRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Review not found."));
        r.setStatus(normalized); ProductReview saved=reviewRepository.save(r); refreshProductRating(r.getProduct().getId()); return toResponse(saved);
    }
    @Transactional
    public void delete(Long id){ProductReview r=reviewRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Review not found.")); Long pid=r.getProduct().getId(); reviewRepository.delete(r); refreshProductRating(pid);}
    @Transactional
    public void refreshProductRating(Long productId){
        Product p=productRepository.findById(productId).orElseThrow(()->new IllegalArgumentException("Product not found."));
        List<ProductReview> approved=reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId,"APPROVED");
        if(approved.isEmpty()){p.setRating(null);p.setReviews(0);} else {double avg=approved.stream().mapToInt(ProductReview::getRating).average().orElse(0);p.setRating(BigDecimal.valueOf(avg).setScale(1,RoundingMode.HALF_UP));p.setReviews(approved.size());}
        productRepository.save(p);
    }
    public ProductReviewResponse toResponse(ProductReview r){ProductReviewResponse x=new ProductReviewResponse();x.setId(r.getId());x.setProductId(r.getProduct().getId());x.setProductName(r.getProduct().getName());x.setCustomerName(maskName(r.getCustomer().getName()));x.setReview(r.getReview());x.setRating(r.getRating());x.setStatus(r.getStatus());x.setVerifiedPurchase(r.getVerifiedPurchase());x.setCreatedAt(r.getCreatedAt());return x;}
    private String maskName(String name){if(name==null||name.isBlank())return "Customer";String[] p=name.trim().split("\\s+");return p[0]+(p.length>1?" "+p[p.length-1].substring(0,1)+".":"");}
}
