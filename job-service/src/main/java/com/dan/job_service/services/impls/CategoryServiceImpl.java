package com.dan.job_service.services.impls;

import com.dan.job_service.dtos.requets.CategoryRequest;
import com.dan.job_service.dtos.responses.CategoryResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.Category;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.CategoryService;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JobRepository jobRepository;

    @Override
    public ResponseMessage create(CategoryRequest categoryRequest) {
        try {
            Category category = Category.builder()
                    .name(categoryRequest.name())
                    .description(categoryRequest.description())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .parentId(categoryRequest.parentId())
                    .build();
            categoryRepository.save(category);
            return ResponseMessage.builder()
                    .status(200)
                    .message("Thêm danh mục thành công")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Thêm danh mục thất bại: " + e.getMessage());
        }
    }

    @Override
    public ResponseMessage update(String id, CategoryRequest categoryRequest) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
            
            category.setName(categoryRequest.name());
            category.setDescription(categoryRequest.description());
            category.setUpdatedAt(LocalDateTime.now());
            category.setParentId(categoryRequest.parentId());
            
            categoryRepository.save(category);
            
            return ResponseMessage.builder()
                    .status(200)
                    .message("Cập nhật danh mục thành công")
                    .build();
        } catch (ResourceNotFoundException e) {
            return ResponseMessage.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            return ResponseMessage.builder()
                    .status(500)
                    .message("Cập nhật danh mục thất bại")
                    .build();
        }
    }

    @Override
    public ResponseMessage delete(String id) {
        return categoryRepository.findById(id).map(category -> {
            category.setDeletedAt(LocalDateTime.now());
            categoryRepository.save(category);

            deleteChildrenCategories(id);
            
            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa danh mục thành công")
                    .build();
        }).orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
    }

    @Override
    public CategoryResponse getCategoryById(String id) {
        return categoryRepository.findById(id)
            .map(this::fromCategoryToCategoryResponse)
            .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
    }

    private CategoryResponse fromCategoryToCategoryResponse(Category category) {
        String parentId = category.getParentId();
        Category pCategory = categoryRepository.findById(parentId)
                .orElse(null);

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parent(fromCategoryToCategoryResponse(pCategory))
                .totalJob(jobRepository.countByCategoryId(category.getId()))
                .build();
    }

    private void deleteChildrenCategories(String parentCategoryId) {
        List<Category> childrenCategories = categoryRepository.findByParentId(parentCategoryId);
        childrenCategories.forEach(childCategory -> {
            childCategory.setDeletedAt(LocalDateTime.now());
            categoryRepository.save(childCategory);
        });
    }

    @Override
    public Page<CategoryResponse> getAllCategories(String keyword, Pageable pageable) {
        return categoryRepository.findAllByNameContainingIgnoreCaseAndDeletedAtIsNull(keyword, pageable)
            .map(this::fromCategoryToCategoryResponse);
    }
}
