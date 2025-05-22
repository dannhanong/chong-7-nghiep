package com.dan.identity_service.services;

import java.util.List;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Education;

public interface EducationService {
    List<Education> getEducationByUsername(String username);
    ResponseMessage create(Education education, String username);
    ResponseMessage update(String id, String username, Education education);
    ResponseMessage delete(String id, String username);
    Education getById(String id, String username);
}
