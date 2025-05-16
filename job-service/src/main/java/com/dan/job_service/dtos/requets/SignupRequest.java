package com.dan.job_service.dtos.requets;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "Tên hiển thị không được để trống")
    @Min(value = 3, message = "Tên hiển thị phải có ít nhất 3 ký tự")
    String name,

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,}$", message = "Tên đăng nhập chỉ chứa ký tự chữ, số và dấu gạch dưới, không chứa khoảng trắng và ít nhất 3 ký tự")
    String username,

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    @Size(min = 10, max = 15, message = "Số điện thoại phải có từ 10 đến 15 ký tự")
    String phoneNumber,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    String password,

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Size(min = 6, message = "Mật khẩu xác nhận phải có ít nhất 6 ký tự")
    String confirmPassword,
    
    String role
) {}
