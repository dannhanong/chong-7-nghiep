package com.dan.job_service.services.impls;

import com.dan.job_service.dtos.requets.CategoryRequest;
import com.dan.job_service.dtos.responses.CategoryResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.Category;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.CategoryService;
import com.dan.job_service.services.JobService;

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

    @Autowired
    private JobService jobService;

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
            // xóa tất cả các công việc liên quản đến danh mục này
            jobService.deleteJobsByCategoryId(id);
           
            
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

    @Override
    public List<CategoryResponse> getCategoriesByParentId(String parentId) {
        categoryRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        
        return categoryRepository.findByParentId(parentId)
            .stream()
            .map(this::fromCategoryToCategoryResponse)
            .toList();
    }


    private CategoryResponse fromCategoryToCategoryResponse(Category category) {
        String parentId = category.getParentId();
        Category pCategory = null;
        if (parentId != null) {
            pCategory = categoryRepository.findById(parentId)
                    .orElse(null);
        }

        // Get child categories
        List<CategoryResponse> childCategories = categoryRepository.findByParentId(category.getId())
                .stream()
                .map(childCategory -> CategoryResponse.builder()
                        .id(childCategory.getId())
                        .name(childCategory.getName())
                        .description(childCategory.getDescription())
                        .totalJob(jobRepository.countByCategoryId(childCategory.getId()))
                        .build())
                .toList();

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parent(pCategory != null ? CategoryResponse.builder()
                        .id(pCategory.getId())
                        .name(pCategory.getName())
                        .description(pCategory.getDescription())
                        .totalJob(jobRepository.countByCategoryId(pCategory.getId()))
                        .build() : null)
                .totalJob(jobRepository.countByCategoryId(category.getId()))
                .childrens(childCategories)
                .build();
    }

    private void deleteChildrenCategories(String parentCategoryId) {
        List<Category> childrenCategories = categoryRepository.findByParentId(parentCategoryId);
        childrenCategories.forEach(childCategory -> {
            childCategory.setDeletedAt(LocalDateTime.now());
            categoryRepository.save(childCategory);
            jobService.deleteJobsByCategoryId(childCategory.getId());

        });
    }

   @Override
    public Page<CategoryResponse> getAllCategories(String keyword, Pageable pageable) {
        Page<Category> categories;

        // Nếu có keyword, tìm theo tên và chưa xóa; nếu không, lấy tất cả chưa xóa
        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryRepository.findAllByNameContainingIgnoreCaseAndDeletedAtNullAndParentIdNull(keyword, pageable);
        } else {
            categories = categoryRepository.findByDeletedAtNullAndParentIdNull(pageable);
        }

        return categories.map(category -> {
            // Lấy danh mục con chưa xóa
            List<CategoryResponse> childCategories = categoryRepository.findByParentIdAndDeletedAtNull(category.getId())
                .stream()
                .map(child -> CategoryResponse.builder()
                    .id(child.getId())
                    .name(child.getName())
                    .description(child.getDescription())
                    .deletedAt(child.getDeletedAt())
                    .totalJob(jobRepository.countByCategoryId(child.getId()))
                    .build())
                .toList();

            // Lấy danh mục cha nếu có và chưa bị xóa
            CategoryResponse parentCategoryResponse = null;
            if (category.getParentId() != null) {
                parentCategoryResponse = categoryRepository.findById(category.getParentId())
                    .filter(parent -> parent.getDeletedAt() == null)
                    .map(parent -> CategoryResponse.builder()
                        .id(parent.getId())
                        .name(parent.getName())
                        .description(parent.getDescription())
                        .deletedAt(parent.getDeletedAt())
                        .totalJob(jobRepository.countByCategoryId(parent.getId()))
                        .build())
                    .orElse(null);
            }

            // Trả về dữ liệu danh mục chính
            return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .deletedAt(category.getDeletedAt())
                .parent(parentCategoryResponse)
                .totalJob(jobRepository.countByCategoryId(category.getId()))
                .childrens(childCategories)
                .build();
        });
    }

}
