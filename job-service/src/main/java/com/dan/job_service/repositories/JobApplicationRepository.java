package com.dan.job_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobApplication;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    Page<JobApplication> findByUserId(String userId, Pageable pageable);
    Page<JobApplication> findByJobId(String jobId, Pageable pageable);

    Optional<JobApplication> findByUserIdAndJobId(String userId, String jobId);
    Page<JobApplication> findByUserIdAndStatus(String userId, ApplicationStatus status, Pageable pageable); // Phương thức mới
    @Query(value = "{'userId': ?0, 'status': 'APPROVED'}", count = true)
    long countApprovedApplicationsByUserId(String userId);
    // Tìm kiếm đơn ứng tuyển theo jobId 
    List<JobApplication> findByJobId(String jobId);
    Integer countByUserIdAndStatus(String userId, ApplicationStatus status);
    Page<JobApplication> findByUserIdAndStatus(String userId, String status, Pageable pageable);

}
