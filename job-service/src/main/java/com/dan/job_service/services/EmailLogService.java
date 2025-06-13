package com.dan.job_service.services;

import com.dan.events.dtos.responses.RecommendJobGmailResponse;
import com.dan.job_service.models.EmailLog;

import java.util.List;

public interface EmailLogService {
    EmailLog saveEmailLog(String email, String jobId);
    // Map<String, List<String>> filterUnsentJobs(Map<String, List<Map<String, String>>> request);
    List<RecommendJobGmailResponse> getGmailJobs();
}
