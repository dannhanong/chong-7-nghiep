package com.dan.identity_service.models;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.identity_service.dtos.enums.Gender;
import com.dan.identity_service.dtos.enums.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "username_index", unique = true, def = "{'username': 1}"),
        @CompoundIndex(name = "email_index", unique = true, def = "{'email': 1}"),
        @CompoundIndex(name = "phone_number_index", unique = true, def = "{'phoneNumber': 1}"),
        @CompoundIndex(name = "verification_code_index", unique = true, def = "{'verificationCode': 1}"),
        @CompoundIndex(name = "reset_password_token_index", unique = true, def = "{'resetPasswordToken': 1}"),
        @CompoundIndex(name = "deleted_at_index", def = "{'deletedAt': 1}"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class User {
    @Id
    String id;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Min(value = 3, message = "Tên hiển thị phải có ít nhất 3 ký tự")
    String name;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,}$", message = "Tên đăng nhập chỉ chứa ký tự chữ, số và dấu gạch dưới, không chứa khoảng trắng và ít nhất 3 ký tự")
    String username;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @JsonIgnore
    String password;
    boolean enabled;

    String verificationCode;
    String resetPasswordToken;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email;

    Set<Role> roles;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    @Size(min = 10, max = 15, message = "Số điện thoại phải có từ 10 đến 15 ký tự")
    @Indexed(unique = true)
    String phoneNumber;

    String avatarCode;
    ProviderType providerType;

    String address;
    boolean subscribedToNotifications;

    int jobCount;
    String title;
    String bio;
    LocalDate dob;
    Gender gender;

    String companyId;
    boolean identityVerified;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
