package com.dan.job_profile_service.dtos.requests;

import com.dan.job_profile_service.dtos.enums.EmploymentType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExperienceRequest {
    String profileId;
    String companyName;
    String position;
    EmploymentType employmentType; //FULL_TIME, PART_TIME, INTERN, COLLABORATOR
    String location;
    LocalDate startDate;
    LocalDate endDate;
    String description;
    String achievements;
}
