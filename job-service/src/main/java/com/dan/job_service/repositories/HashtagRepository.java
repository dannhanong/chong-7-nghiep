package com.dan.job_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dan.job_service.models.Hashtag;


public interface HashtagRepository extends MongoRepository<Hashtag, String> {
    List<Hashtag> findByTagContainingIgnoreCase(String keyword); 

}
