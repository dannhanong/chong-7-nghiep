package com.dan.identity_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.models.IdentityInfo;

@Repository
public interface IdentityInfoRepository extends MongoRepository<IdentityInfo, String> {
    Optional<List<IdentityInfo>> findByUserId(String userId);
}
