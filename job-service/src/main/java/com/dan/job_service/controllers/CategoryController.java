package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.CategoryRequest;
import com.dan.job_service.dtos.responses.CategoryResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping("/admin/create")
    public ResponseEntity<ResponseMessage> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.create(categoryRequest));
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<ResponseMessage> updateCategory(@PathVariable String id, @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteCategory(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.delete(id));
    }

    @GetMapping("/public/get/{id}")
    public ResponseEntity<?> getCategory(@PathVariable String id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi lấy thông tin danh mục: " + e.getMessage()));
        }
    }

    @GetMapping("/public/get-by-parentId/{id}")
    public ResponseEntity<?> getCategoriesByParentId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoriesByParentId(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi lấy thông tin danh mục: " + e.getMessage()));
        }
    }

    @GetMapping("/public/get-all")
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(categoryService.getAllCategories(keyword, pageable));
    }
}
