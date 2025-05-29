package com.dan.job_service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dan.job_service.dtos.requets.CategoryRequest;
import com.dan.job_service.dtos.responses.CategoryResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;

public interface CategoryService {
    ResponseMessage create(CategoryRequest categoryRequest);
    ResponseMessage update(String id, CategoryRequest categoryRequest);
    ResponseMessage delete(String id);
    CategoryResponse getCategoryById(String id);
    Page<CategoryResponse> getAllCategories(String keyword, Pageable pageable);
}
