package com.dan.job_service.services;

import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobProgress;

public interface JobProgressService {
    ResponseMessage updateProgress(String jobId, String username, String status);
    JobProgress getLatestProgress(String jobId);

}
