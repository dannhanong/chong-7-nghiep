package com.dan.job_profile_service.repositories;

import com.dan.job_profile_service.models.Experience;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends MongoRepository<Experience, String> {
    List<Experience> findByUserId(String userId);
}
