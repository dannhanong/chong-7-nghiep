package com.dan.identity_service.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.models.BlacklistedToken;

import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends MongoRepository<BlacklistedToken, String>{
    Optional<BlacklistedToken> findByToken(String token);
}
