package com.dan.job_service.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.JobEmbedding;

@Repository
public interface JobEmbeddingRepository extends MongoRepository<JobEmbedding, String>{
    
}
