package com.dan.job_service.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobApplication;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    Page<JobApplication> findByUserId(String userId, Pageable pageable);
    Page<JobApplication> findByJobId(String jobId, Pageable pageable);
    Optional<JobApplication> findByUserIdAndJobId(String userId, String jobId);
}
