package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.CategoryRequest;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
}
