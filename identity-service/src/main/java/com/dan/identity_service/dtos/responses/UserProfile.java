package com.dan.identity_service.dtos.responses;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.dan.identity_service.dtos.enums.Gender;
import com.dan.identity_service.models.SolanaTransaction;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {
    String name;
    String username;
    boolean enabled;
    String email;
    String phoneNumber;
    String address;
    String role;
    String avatarCode;
    List<SolanaTransaction> transactions;
    String title;
    String bio;
    LocalDate dob;
    Gender gender;
    boolean subscribedToNotifications;
    LocalDateTime createdAt;
}
