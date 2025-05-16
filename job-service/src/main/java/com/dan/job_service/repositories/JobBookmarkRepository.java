package com.dan.job_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobBookmark;

@Repository
public interface JobBookmarkRepository extends MongoRepository<JobBookmark, String> {
    List<JobBookmark> findByUserIdAndActiveTrue(String userId);
    Optional<JobBookmark> findByUserIdAndJobId(String userId, String jobId);   
    boolean existsByUserIdAndJobId(String userId, String jobId);
    Page<JobBookmark> findByUserIdAndActiveTrue(String userId, Pageable pageable);
}
