package com.dan.job_service.services;

import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.JobApplicationWithJobResponse;
import com.dan.job_service.dtos.responses.JobApplicationProfileResponse;
import com.dan.job_service.dtos.responses.JobApplicationResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface JobApplicationService {
    ResponseMessage applyJob(JobApplicationRequest request, String jobId, String username);

    ResponseMessage updateStatus(String id, String status);

    Page<JobApplicationResponse> getJobApplicationByUserId(String username, Pageable pageable);

    Page<JobApplicationResponse> getJobApplicationByJobId(String jobId, String username, Pageable pageable);

    Page<JobApplicationProfileResponse> getPublicJobApplicationByJobId(String jobId, Pageable pageable);

    long countAppliedSuccess(String userId);

    Object getJobApplicationDetail(String applicationId);

    // xóa đơn ứng tuyển theo jobId
    void deleteByJobId(String jobId);

    ResponseMessage delete(String id);
    

}
