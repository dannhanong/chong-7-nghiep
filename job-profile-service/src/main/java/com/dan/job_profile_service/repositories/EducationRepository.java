package com.dan.job_profile_service.repositories;

import com.dan.job_profile_service.models.Education;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends MongoRepository<Education, String> {
    List<Education> findByUserId(String userId);
}
