package com.dan.job_profile_service.services;

import com.dan.job_profile_service.dtos.requests.ProfileRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Profile;

import java.util.List;

public interface ProfileService {
    List<Profile> getAllProfiles();
    Profile getProfileById(String id);
    Profile create(ProfileRequest profileRequest);
    Profile update(ProfileRequest profileRequest, String id);
    ResponseMessage delete(String id);
}
