package com.dan.job_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobView;

@Repository
public interface JobViewRepository extends MongoRepository<JobView, String> {
    List<JobView> findByUserIdOrderByViewedAtDesc(String userId);
    List<JobView> findByUserIdAndViewDurationGreaterThan(String userId, Integer minDuration);
}
