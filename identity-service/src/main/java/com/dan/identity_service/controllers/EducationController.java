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
import com.dan.identity_service.models.Education;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.EducationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/identity/education")
public class EducationController {
    @Autowired
    private EducationService educationService;
    @Autowired
    private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyEducation(HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            return ResponseEntity.ok(educationService.getEducationByUsername(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi lấy thông tin học vấn: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createEducation(@Valid @RequestBody Education education, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = educationService.create(education, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi thêm thông tin học vấn: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateEducation(@Valid @RequestBody Education education, HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = educationService.update(id, username, education);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi cập nhật thông tin học vấn: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEducation(HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = educationService.delete(id, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi xóa thông tin học vấn: " + e.getMessage()));
        }
    }
}
