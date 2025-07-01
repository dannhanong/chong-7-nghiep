package com.dan.job_profile_service.controllers;

import com.dan.job_profile_service.dtos.requests.ExperienceRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Experience;
import com.dan.job_profile_service.models.Skill;
import com.dan.job_profile_service.security.jwt.JwtService;
import com.dan.job_profile_service.services.ExperienceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jp/experiences")
@RequiredArgsConstructor
public class ExperienceController {
    private final JwtService jwtService;
    private final ExperienceService experienceService;

    @PostMapping("/create")
    public ResponseEntity<ResponseMessage> createExperience(
            HttpServletRequest request,
            @Valid @ModelAttribute ExperienceRequest experienceRequest
    ) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            experienceService.create(experienceRequest, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Thêm kinh nghệm làm việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi thêm kinh nghiệm: " + e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<List<Experience>> getAllExperiences(HttpServletRequest request) {
        String username = jwtService.getUsernameFromRequest(request);
        return ResponseEntity.ok(experienceService.getAllExperiences(username));
    }

    @GetMapping("/get/{id}")
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

    @PutMapping("/update/{id}")
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

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteExperience(@PathVariable String id) {
        return ResponseEntity.ok(experienceService.delete(id));
    }

    @GetMapping("public/get/{userId}")
    public ResponseEntity<?> getExperienceByUserId(@PathVariable String userId) {
        try {
            List<Experience> experiences = experienceService.getExperienceByUserId(userId);
            return ResponseEntity.ok(experiences);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage(400, "Không tìm thấy kinh nghiệm: " + e.getMessage()));
        }
    }
}
