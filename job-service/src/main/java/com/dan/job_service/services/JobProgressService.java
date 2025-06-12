package com.dan.job_service.services;

import com.dan.job_service.dtos.responses.ResponseMessage;

public interface JobProgressService {
    ResponseMessage updateProgress(String jobId, String username, String status);
}
