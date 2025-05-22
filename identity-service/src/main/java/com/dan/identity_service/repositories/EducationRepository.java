package com.dan.identity_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.models.Education;

@Repository
public interface EducationRepository extends MongoRepository<Education, String>{
    List<Education> findByUserIdAndDeletedAtNull(String userId);
    boolean existsByIdAndUserIdAndDeletedAtNull(String id, String userId);
    Education findByIdAndUserIdAndDeletedAtNull(String id, String userId);
}
