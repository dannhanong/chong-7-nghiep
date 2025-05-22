package com.dan.identity_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.models.Skill;

@Repository
public interface SkillRepository extends MongoRepository<Skill, String> {
    List<Skill> findByUserIdAndDeletedAtNull(String userId);
    boolean existsByIdAndUserIdAndDeletedAtNull(String id, String userId);
    Skill findByIdAndUserIdAndDeletedAtNull(String id, String userId);
}
