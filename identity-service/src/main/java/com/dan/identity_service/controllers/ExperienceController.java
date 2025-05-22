package com.dan.identity_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Experience;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.ExperienceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/identity/experiences")
public class ExperienceController {
    @Autowired
    private ExperienceService experienceService;
    @Autowired
    private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyExperience(HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            return ResponseEntity.ok(experienceService.getExperienceByUsername(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi lấy thông tin kinh nghiệm làm việc: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createExperience(@Valid @RequestBody Experience experience, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = experienceService.create(experience, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi thêm thông tin kinh nghiệm: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateExperience(@Valid @RequestBody Experience experience, HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = experienceService.update(id, username, experience);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi cập nhật thông tin kinh nghiệm: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteExperience(HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = experienceService.delete(id, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi xóa thông tin kinh nghiệm: " + e.getMessage()));
        }
    }
}
