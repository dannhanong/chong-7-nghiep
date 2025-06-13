package com.dan.job_profile_service.services;

import com.dan.job_profile_service.dtos.requests.ExperienceRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Experience;

import java.util.List;

public interface ExperienceService {
    List<Experience> getAllExperiences(String username);

    Experience getExperienceById(String id);

    Experience create(ExperienceRequest experienceRequest, String username);

    Experience update(ExperienceRequest experienceRequest, String id);

    ResponseMessage delete(String id);

    List<Experience> getExperienceByUserId(String userId);
}
