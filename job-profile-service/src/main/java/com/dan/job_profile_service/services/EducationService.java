package com.dan.job_profile_service.services;

import com.dan.job_profile_service.dtos.requests.EducationRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Education;

import java.util.List;

public interface EducationService {
    List<Education> getAllEducations();
    Education getEducationById(String id);
    Education create(EducationRequest educationRequest);
    Education update(EducationRequest educationRequest, String id);
    ResponseMessage delete(String id);
}
