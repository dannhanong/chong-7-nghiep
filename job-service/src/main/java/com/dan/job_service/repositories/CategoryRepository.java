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
    Page<Category> findAllByNameContainingIgnoreCaseAndDeletedAtNull(String name, Pageable pageable);
    Page<Category> findAllByNameContainingIgnoreCaseAndDeletedAtNullAndParentIdNull(String name, Pageable pageable);
    // Tìm tất cả danh mục chưa bị xóa
    Page<Category> findByDeletedAtNull(Pageable pageable);
    Page<Category> findByDeletedAtNullAndParentIdNull(Pageable pageable);

    // Tìm danh mục con chưa bị xóa
    List<Category> findByParentIdAndDeletedAtNull(String parentId);
}
