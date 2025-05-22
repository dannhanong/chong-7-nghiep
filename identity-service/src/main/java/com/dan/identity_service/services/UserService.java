package com.dan.identity_service.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.dan.identity_service.dtos.requests.UpdateProfileRequest;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.dtos.responses.UserProfile;
import com.dan.identity_service.models.User;

public interface UserService extends UserDetailsService {
    User getByUsername(String username);
    User getById(String id);
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    boolean isEnableUser(String username);
    UserProfile getProfile(String username);
    User oauth2Authenticate(String code);
    User getUserByUsername(String username);
    void plusJobCount(String userId);
    void minusJobCount(String userId);
    ResponseMessage updateProfile(UpdateProfileRequest updateProfileRequest, String username);
    void updateCompanyIdForOwner(String ownerId, String companyId);
}
