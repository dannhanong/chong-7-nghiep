package com.dan.job_profile_service.dtos.requests;

import com.dan.job_profile_service.dtos.enums.EmploymentType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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
    String employmentType; //FULL_TIME, PART_TIME, INTERN, COLLABORATOR
    String location;
    LocalDate startDate;
    LocalDate endDate;
    String description;
    String achievements;
    MultipartFile file;
    List<MultipartFile> otherFiles;
}
