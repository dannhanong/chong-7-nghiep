package com.dan.job_service.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.SearchClick;

@Repository
public interface SearchClickRepository extends MongoRepository<SearchClick, String> {
    List<SearchClick> findByUserIdAndTimestampAfter(String userId, LocalDateTime since);
}
