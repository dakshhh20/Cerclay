package com.mittiandmore.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(
                        name = "idx_products_category",
                        columnList = "category"
                ),
                @Index(
                        name = "idx_products_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_products_slug",
                        columnList = "slug",
                        unique = true
                ),
                @Index(
                        name = "idx_products_sku",
                        columnList = "sku",
                        unique = true
                )
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean archived = false;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "set_of_2_enabled", nullable = false)
    private Boolean setOf2Enabled = false;

    @Column(name = "set_of_2_price", precision = 10, scale = 2)
    private BigDecimal setOf2Price;

    @Column(name = "set_of_2_mrp", precision = 10, scale = 2)
    private BigDecimal setOf2Mrp;

    @Column(name = "color_group", length = 150)
    private String colorGroup;

    @Column(name = "color_name", length = 80)
    private String colorName;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false)
    private Integer reviews = 0;

    @Column(columnDefinition = "TEXT")
    private String image;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    /*
     * Used for optimistic locking.
     *
     * Hibernate automatically increments this value whenever
     * the product is updated.
     *
     * This helps prevent two requests from simultaneously
     * overwriting each other's stock changes.
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product() {
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Boolean getSetOf2Enabled() { return setOf2Enabled; }
    public void setSetOf2Enabled(Boolean setOf2Enabled) { this.setOf2Enabled = setOf2Enabled; }
    public BigDecimal getSetOf2Price() { return setOf2Price; }
    public void setSetOf2Price(BigDecimal setOf2Price) { this.setOf2Price = setOf2Price; }
    public BigDecimal getSetOf2Mrp() { return setOf2Mrp; }
    public void setSetOf2Mrp(BigDecimal setOf2Mrp) { this.setOf2Mrp = setOf2Mrp; }
    public String getColorGroup() { return colorGroup; }
    public void setColorGroup(String colorGroup) { this.colorGroup = colorGroup; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviews() {
        return reviews;
    }

    public void setReviews(Integer reviews) {
        this.reviews = reviews;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}