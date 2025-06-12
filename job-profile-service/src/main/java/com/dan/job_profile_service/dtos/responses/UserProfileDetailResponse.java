package com.dan.job_profile_service.dtos.responses;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDetailResponse {
    private String userId;
    private String name;
    private boolean enabled;
    private String email;
    private String roles;
    private String linkPage;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date dob;
    private String phoneNumber;
    private String avatarId;
    private String pathName;
}
