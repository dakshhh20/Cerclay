package com.mittiandmore.repository;

import com.mittiandmore.entity.ReturnPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnPhotoRepository extends JpaRepository<ReturnPhoto, Long> {
    List<ReturnPhoto> findByReturnRequestIdOrderByCreatedAtAsc(Long returnRequestId);
    long countByReturnRequestId(Long returnRequestId);
}
