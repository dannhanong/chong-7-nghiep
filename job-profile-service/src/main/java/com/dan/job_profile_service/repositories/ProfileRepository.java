package com.dan.job_profile_service.repositories;

import com.dan.job_profile_service.models.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends MongoRepository<Profile, String> {
}
