package com.dan.job_profile_service.dtos.requests;

import com.dan.job_profile_service.dtos.enums.ProficiencyType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillRequest {
    String skillName;
    ProficiencyType proficiency;
    Double yearsExperience;
    String certifications;
    String description;
    MultipartFile file;
}
