package com.dan.job_service.services;

import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.ApplicantResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobApplicationService {
    ResponseMessage applyJob(JobApplicationRequest request, String jobId, String username);
    Page<JobApplication> getJobApplicationByUserId( String username, Pageable pageable);
    Page<JobApplication> getJobApplicationByJobId(String jobId, String username, Pageable pageable);
}
