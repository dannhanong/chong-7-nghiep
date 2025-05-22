package com.dan.identity_service.dtos.requests;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword,
    String confirmPassword
) {}
