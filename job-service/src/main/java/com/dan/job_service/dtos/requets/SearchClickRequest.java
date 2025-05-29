package com.dan.job_service.dtos.requets;

import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

public record SearchClickRequest(
        String userId,
        String jobId,
        String searchQuery,
        Integer positionInResults,
        LocalDateTime timestamp
) {
}
