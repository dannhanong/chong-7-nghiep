package com.dan.job_service.repositories;

import com.dan.job_service.models.JobProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobProgressRepository extends MongoRepository<JobProgress, String> {
}
