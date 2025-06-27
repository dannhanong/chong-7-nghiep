package com.dan.job_service.dtos.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.dan.job_service.dtos.enums.ApplicationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationProfileResponse {
      String id;
     String userId;
     String name;
     boolean enabled;
     String email;
     Date dob;
     String avatarId;
     String pathName;
     Double averageRating;
      LocalDateTime appliedAt;
     String offerPlan;
     long offerSalary;
     String offerSkill;
     Integer totalCountJobDone;
     List<SkillResponse> skills;
     ApplicationStatus status;

}
