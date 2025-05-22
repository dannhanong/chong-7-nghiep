package com.dan.identity_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dan.identity_service.models.SolanaTransaction;

public interface SolanaTransactionRepository extends MongoRepository<SolanaTransaction, String> {
    List<SolanaTransaction> findByUserId(String userId);
}
