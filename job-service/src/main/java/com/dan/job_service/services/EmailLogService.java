package com.dan.job_service.services;

import com.dan.job_service.models.EmailLog;

import java.util.List;

public interface EmailLogService {
    EmailLog saveEmailLog(String username, String jobId);
    List<String> filterUnsentJobs(String username, List<String> jobIds);
}
