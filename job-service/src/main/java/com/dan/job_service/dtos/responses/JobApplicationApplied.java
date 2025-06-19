package com.dan.job_service.dtos.responses;

import java.time.LocalDate;
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
public class JobApplicationApplied {
    String id;
    String title;
    String shortDescription; // Changed from description to shortDescription
    long salaryMin;
    long salaryMax;
    LocalDate applicationDeadline;
    String status;
    String statusApplication;
    Boolean done;

}
