package com.dan.identity_service.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dan.identity_service.dtos.requests.LoginRequest;
import com.dan.identity_service.dtos.requests.SignupRequest;
import com.dan.identity_service.dtos.responses.LoginResponse;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.dtos.responses.UserProfile;
import com.dan.identity_service.models.User;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.AccountService;
import com.dan.identity_service.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/identity/auth")
public class AuthController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage> signup(@Valid @RequestBody SignupRequest signupRequest) {        
        try {
            accountService.signup(signupRequest);
            return ResponseEntity.ok(new ResponseMessage(200, "Đăng ký thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi đăng ký người dùng: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            return ResponseEntity.ok(accountService.login(loginRequest));
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseMessage(400, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        try {
            ResponseMessage responseMessage = accountService.verify(token);
            if (responseMessage.getStatus() == 200) {
                return ResponseEntity.ok(responseMessage);
            } else {
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi xác thực: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        List<String> roles = jwtService.extractClaim(refreshToken, claims -> claims.get("roles", List.class));
        String newAccessToken = jwtService.generateToken(username, roles);
        String newRefreshToken = jwtService.generateRefreshToken(username, roles);
        
        LoginResponse tokens = LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        jwtService.validateToken(token);
        return "true";
    }

    @GetMapping("/private/get/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/public/get/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userService.getById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            UserProfile userProfile = userService.getProfile(username);
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi lấy thông tin người dùng: " + e.getMessage()));
        }
    }
}
