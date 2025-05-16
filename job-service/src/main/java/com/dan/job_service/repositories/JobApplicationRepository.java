package com.dan.job_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobApplication;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    List<JobApplication> findByUserId(String userId);
    List<JobApplication> findByJobId(String jobId);
    Optional<JobApplication> findByUserIdAndJobId(String userId, String jobId);
}
