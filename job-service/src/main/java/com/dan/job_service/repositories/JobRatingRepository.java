package com.dan.job_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobRating;

@Repository
public interface JobRatingRepository extends MongoRepository<JobRating, String> {
    List<JobRating> findByUserId(String userId);
    Optional<JobRating> findByUserIdAndJobId(String userId, String jobId);
    // Double averageByJobId(String jobId);
}
