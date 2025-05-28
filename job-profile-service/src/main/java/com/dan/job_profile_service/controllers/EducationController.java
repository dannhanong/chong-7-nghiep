package com.dan.job_profile_service.controllers;

import com.dan.job_profile_service.dtos.requests.EducationRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Education;
import com.dan.job_profile_service.services.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jp/educations")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @PostMapping("/create")
    public ResponseEntity<ResponseMessage> createEducation(
            @Valid @RequestBody EducationRequest educationRequest
    ) {
        try {
            educationService.create(educationRequest);
            return ResponseEntity.ok(new ResponseMessage(200, "Thêm học vấn thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi thêm học vấn: " + e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<List<Education>> getAllEducations() {
        List<Education> educations = educationService.getAllEducations();
        return ResponseEntity.ok(educations);
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getEducationById(@PathVariable String id) {
        try {
            Education edu = educationService.getEducationById(id);
            return ResponseEntity.ok(edu);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage(400, "Không tìm thấy bản ghi education với id: " + id));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseMessage> updateEducation(
            @Valid @RequestBody EducationRequest educationRequest,
            @PathVariable String id
    ) {
        try {
            educationService.update(educationRequest, id);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật học vấn thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi cập nhật học vấn: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteEducation(@PathVariable String id) {
        ResponseMessage resp = educationService.delete(id);
        return ResponseEntity.status(resp.getStatus()).body(resp);
    }
}
