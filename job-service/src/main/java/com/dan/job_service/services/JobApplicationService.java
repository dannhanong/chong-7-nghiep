package com.dan.job_service.services;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.JobApplicationWithJobResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobApplicationService {
    ResponseMessage applyJob(JobApplicationRequest request, String jobId, String username);
    ResponseMessage updateStatus(String id, String status);
    Page<JobApplication> getJobApplicationByUserId(String username, Pageable pageable);
    Page<JobApplication> getJobApplicationByJobId(String jobId, String username, Pageable pageable);
    Page<JobApplicationWithJobResponse> getJobApplicationsWithJobByUserId(String username, ApplicationStatus status, Pageable pageable); // Cập nhật phương thức
    Object getJobApplicationDetail(String applicationId);
}
