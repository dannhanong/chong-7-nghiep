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
import com.dan.identity_service.models.Skill;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.SkillService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/identity/skills")
public class SkillController {
    @Autowired
    private SkillService skillService;
    @Autowired
    private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getMySkills(HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            return ResponseEntity.ok(skillService.getSkillByUsername(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi lấy thông tin kỹ năng: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSkill(@Valid @RequestBody Skill skill, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = skillService.create(skill, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi thêm thông tin kỹ năng: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSkill(@Valid @RequestBody Skill skill, HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = skillService.update(id, username, skill);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi cập nhật thông tin kỹ năng: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSkill(HttpServletRequest request, @PathVariable String id) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = skillService.delete(id, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi xóa thông tin kỹ năng: " + e.getMessage()));
        }
    }
}
