package com.dan.job_service.services;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;

public interface JobService {
    ResponseMessage create(JobRequest jobRequest, String username);
    ResponseMessage update(String id, JobRequest jobRequest, String username);
    ResponseMessage delete(String id, String username);
    JobDetail getJobById(String id, String username);
}
