package com.dan.identity_service.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dan.identity_service.dtos.requests.SignupRequest;
import com.dan.identity_service.dtos.requests.StaffAccountRequest;
import com.dan.identity_service.dtos.requests.UpdateProfileRequest;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.User;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.AccountService;
import com.dan.identity_service.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/identity/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private JwtService jwtService;

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @ModelAttribute UpdateProfileRequest updateProfileRequest, 
                                            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = userService.updateProfile(updateProfileRequest, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi cập nhật thông tin người dùng: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/create")
    public ResponseEntity<Map<String, String>> createByAdmin(@Valid @RequestBody SignupRequest signupRequest) {        
        try {
            User user = accountService.createByAdmin(signupRequest);
            Map<String, String> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            return ResponseEntity.ok(userMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorMap);        
        }
    }

    @PostMapping("/recruiter/create-staff")
    public ResponseEntity<?> createStaffAccount(@Valid @RequestBody StaffAccountRequest staffAccountRequest, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage responseMessage = accountService.createStaffAccount(staffAccountRequest, username);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi tạo tài khoản nhân viên: " + e.getMessage()));
        }
    }
}
