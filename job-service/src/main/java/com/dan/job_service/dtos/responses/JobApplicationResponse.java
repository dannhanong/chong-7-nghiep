package com.dan.job_service.dtos.responses;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {
    private String id;
    private String userId;
    private String jobId;
    private String userName;
    private String title;
    private ApplicationStatus status;
    private long offerSalary;
    private String offerPlan;
    private String offerSkill;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
} 