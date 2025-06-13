package com.dan.job_service.dtos.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileDetail {
     String profileId;
     String userId;
    String name;
    boolean enabled;
    String email;
    String roles;
    String linkPage;
    Date dob;
    String phoneNumber;
    String avatarId;
    String pathName;
    Double averageRating;
     List<SkillResponse> skills;

    List<ExperienceResponse> experiences;
}
