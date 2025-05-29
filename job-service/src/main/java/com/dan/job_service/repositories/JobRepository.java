package com.dan.job_service.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.Job;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    Integer countByCategoryId(String categoryId);

    List<Job> findByCreatedAtBetweenAndActiveTrue(LocalDateTime start, LocalDateTime end);
}
