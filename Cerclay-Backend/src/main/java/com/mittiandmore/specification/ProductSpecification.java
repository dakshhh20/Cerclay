package com.mittiandmore.specification;

import com.mittiandmore.entity.Product;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), pattern)
            );
        };
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null || category.isBlank()) {
                return null;
            }

            return criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.trim().toLowerCase());
        };
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return null;
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return null;
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> hasColorGroup(String colorGroup) {
        return (root, query, criteriaBuilder) -> {
            if (colorGroup == null || colorGroup.isBlank()) {
                return null;
            }

            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("colorGroup")),
                colorGroup.trim().toLowerCase()
            );
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
    }

    public static Specification<Product> hasStock() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("stock"), 0);
    }
}
