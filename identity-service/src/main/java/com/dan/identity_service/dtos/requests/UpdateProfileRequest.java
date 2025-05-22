package com.dan.identity_service.dtos.requests;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Tên hiển thị không được để trống")
    @Min(value = 3, message = "Tên hiển thị phải có ít nhất 3 ký tự")
    String name,

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    @Size(min = 10, max = 15, message = "Số điện thoại phải có từ 10 đến 15 ký tự")
    @Indexed(unique = true)
    String phoneNumber,
    String address,
    boolean subscribedToNotifications,
    String title,
    String bio,
    LocalDate dob,
    String gender,
    MultipartFile avatar
) {}