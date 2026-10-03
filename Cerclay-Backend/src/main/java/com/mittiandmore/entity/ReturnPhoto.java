package com.mittiandmore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_photos", indexes = {
        @Index(name = "idx_return_photos_return", columnList = "return_request_id")
})
public class ReturnPhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;
    @Column(name = "content_type", length = 100)
    private String contentType;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
    public Long getId(){return id;}
    public ReturnRequest getReturnRequest(){return returnRequest;} public void setReturnRequest(ReturnRequest v){returnRequest=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getOriginalFileName(){return originalFileName;} public void setOriginalFileName(String v){originalFileName=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
