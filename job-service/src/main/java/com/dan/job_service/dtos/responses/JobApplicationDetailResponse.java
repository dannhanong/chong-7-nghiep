package com.dan.job_service.dtos.responses;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDetailResponse {
    private String id;
    private String jobId;
    private String jobTitle;
    private String clientUsername;
    private String freelancerUsername;
    private String status;
    private Instant completedAt;
}