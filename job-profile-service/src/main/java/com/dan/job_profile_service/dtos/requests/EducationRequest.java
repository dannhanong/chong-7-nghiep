package com.dan.job_profile_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EducationRequest {
    String profileId;
    String schoolName;
    String degree;
    String major;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Double grade;
    String location;
}
