package com.dan.job_service.dtos.requets;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

public record JobApplicationRequest(
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt,
        long offerSalary,
        String offerPlan,
        String offerSkill
) {
}
