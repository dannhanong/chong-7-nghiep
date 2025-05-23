package com.dan.identity_service.services;

import com.dan.identity_service.dtos.requests.LoginRequest;
import com.dan.identity_service.dtos.requests.SignupRequest;
import com.dan.identity_service.dtos.requests.StaffAccountRequest;
import com.dan.identity_service.dtos.responses.LoginResponse;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.User;

public interface AccountService {
    User signup(SignupRequest signupRequest);
    User createByAdmin(SignupRequest signupRequest);
    ResponseMessage verify(String token);
    LoginResponse login(LoginRequest loginRequest);
    ResponseMessage createStaffAccount(StaffAccountRequest StaffAccountRequest, String username);
}
