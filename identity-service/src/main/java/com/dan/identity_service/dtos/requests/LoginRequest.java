package com.dan.identity_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,}$", message = "Tên đăng nhập chỉ chứa ký tự chữ, số và dấu gạch dưới, không chứa khoảng trắng và ít nhất 3 ký tự")
    String username,
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    String password
) {}
