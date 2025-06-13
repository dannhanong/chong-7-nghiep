package com.dan.job_service.repositories;

import com.dan.job_service.models.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
    EmailLog findByJobId(String jobId);
    List<EmailLog> findByUsername(String username);
    EmailLog findByUsernameAndJobId(String username, String jobId);
    boolean existsByUsernameAndJobId(String username, String jobId);
}
