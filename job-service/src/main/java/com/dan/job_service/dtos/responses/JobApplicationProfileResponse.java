package com.dan.job_service.dtos.responses;

import lombok.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationProfileResponse {
     String userId;
     String name;
     boolean enabled;
     String email;
     Date dob;
     String avatarId;
     String pathName;
     Double averageRating;
     long offerSalary;
     String offerPlan;
     String offerSkill;
     Integer totalCountJobDone;
     List<SkillResponse> skills;

}
