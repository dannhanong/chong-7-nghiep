package com.dan.job_service.dtos.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class JobDetailEmail {
    String id;
    String title;
    String userName;
    String categoryName;
    long salaryMin;
    long salaryMax;
    LocalDate applicationDeadline;
    String description;
}