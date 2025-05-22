package com.dan.identity_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.models.Experience;

@Repository
public interface ExperienceRepository extends MongoRepository<Experience, String> {
    List<Experience> findByUserIdAndDeletedAtNull(String userId);
    boolean existsByIdAndUserIdAndDeletedAtNull(String id, String userId);
    Experience findByIdAndUserIdAndDeletedAtNull(String id, String userId);
}
