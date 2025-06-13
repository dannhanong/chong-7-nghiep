package com.dan.job_service.dtos.responses;

import java.time.LocalDate;

import com.dan.job_service.dtos.enums.WorkingForm;
import com.dan.job_service.dtos.enums.WorkingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class JobDetail {
    String id;
    String userName;
    String name;
    
    String categoryName; // Added field
    String categoryId; // Added field
    String userId;
    String title;
    String shortDescription; // Changed from description to shortDescription
    String description;
    long salaryMin;
    long salaryMax;
    String experienceLevel;
    String benefits;
    LocalDate applicationDeadline;
    Boolean status;
    Boolean active;
    String createdAt;
    String updatedAt;
    String contentUri;
    WorkingType workingType;
    WorkingForm workingForm;
    String file;
    Integer sumJob;
}
