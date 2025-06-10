package com.dan.job_service.services;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.Job;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {
    ResponseMessage create(JobRequest jobRequest, String username);

    ResponseMessage update(String id, JobRequest jobRequest, String username);

    ResponseMessage delete(String id, String username);

    JobDetail getJobById(String id, String username);

    List<JobsLast24HoursResponse> getJobsPostedLast24Hours();

    Page<JobDetail> getAll(String categoryId, String title, Pageable pageable); // Updated for pagination and filtering
    Page<JobDetail> getJobsByUserId(String username, Pageable pageable);
    ResponseMessage userUpdateJob(String id, JobRequest jobRequest, String username);
    Page<Job> getJobsCategoryId(String categoryId, Pageable pageable);
    
    ResponseMessage markJobAsDone(String jobId, String username);
    
    ResponseMessage markJobAsUndone(String jobId, String username);
}