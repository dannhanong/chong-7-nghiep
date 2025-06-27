package com.dan.job_service.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);
    Page<Category> findAllByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);
    // Tìm tất cả danh mục chưa bị xóa
    Page<Category> findByDeletedAtIsNull(Pageable pageable);

    // Tìm danh mục con chưa bị xóa
    List<Category> findByParentIdAndDeletedAtIsNull(String parentId);
}
