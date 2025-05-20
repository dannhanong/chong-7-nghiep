package com.dan.job_profile_service.controllers;

import com.dan.job_profile_service.dtos.requests.ExperienceRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Experience;
import com.dan.job_profile_service.services.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @PostMapping("/admin/create")
    public ResponseEntity<ResponseMessage> createExperience(
            @Valid @RequestBody ExperienceRequest experienceRequest
    ) {
        try {
            experienceService.create(experienceRequest);
            return ResponseEntity.ok(new ResponseMessage(200, "Thêm kinh nghệm làm việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi thêm kinh nghiệm: " + e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<List<Experience>> getAllExperiences() {
        return ResponseEntity.ok(experienceService.getAllExperiences());
    }

    @GetMapping("/admin/get/{id}")
    public ResponseEntity<?> getExperienceById(@PathVariable String id) {
        try {
            Experience exp = experienceService.getExperienceById(id);
            return ResponseEntity.ok(exp);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage(400, "Không tìm thấy kinh nghiệm với id: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<ResponseMessage> updateExperience(
            @Valid @RequestBody ExperienceRequest experienceRequest,
            @PathVariable String id
    ) {
        try {
            experienceService.update(experienceRequest, id);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật kinh nghiệm thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi cập nhật kinh nghiệm làm việc: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteExperience(@PathVariable String id) {
        return ResponseEntity.ok(experienceService.delete(id));
    }
}
