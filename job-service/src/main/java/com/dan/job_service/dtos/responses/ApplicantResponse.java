package com.dan.job_service.dtos.responses;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicantResponse {
    private String userId;
    private String username;
    private String fullName; // Lấy từ UserDetailToCreateJob
    private String email;    // Lấy từ UserDetailToCreateJob
    private String jobId;
    private ApplicationStatus status;
    private Double offerSalary;
    private String offerPlan;
    private String offerSkill;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
