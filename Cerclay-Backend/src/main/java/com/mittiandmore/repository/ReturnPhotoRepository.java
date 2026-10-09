package com.mittiandmore.repository;

import com.mittiandmore.entity.ReturnPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnPhotoRepository extends JpaRepository<ReturnPhoto, Long> {
    List<ReturnPhoto> findByReturnRequestIdOrderByCreatedAtAsc(Long returnRequestId);
    long countByReturnRequestId(Long returnRequestId);
}
