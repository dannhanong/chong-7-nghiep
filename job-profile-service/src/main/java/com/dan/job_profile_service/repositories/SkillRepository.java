package com.dan.job_profile_service.repositories;

import com.dan.job_profile_service.models.Skill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends MongoRepository<Skill, String> {
    List<Skill> findByUserId(String userId);
}
