package com.dan.identity_service.services;

import java.util.List;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Experience;

public interface ExperienceService {
    List<Experience> getExperienceByUsername(String username);
    ResponseMessage create(Experience experience, String username);
    ResponseMessage update(String id, String username, Experience experience);
    ResponseMessage delete(String id, String username);
    Experience getById(String id, String username);
}
