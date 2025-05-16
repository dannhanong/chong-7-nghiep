package com.dan.job_service.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.UserInteraction;

@Repository
public interface UserInteractionRepository extends MongoRepository<UserInteraction, String> {
    List<UserInteraction> findByUserIdAndItemType(String userId, String itemType);
    List<UserInteraction> findByUserIdAndInteractionType(String userId, String interactionType);
    List<UserInteraction> findByUserIdAndTimestampAfter(String userId, LocalDateTime since);
}
