package com.dan.job_service.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.job_service.models.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByParentId(String parentId);
}
