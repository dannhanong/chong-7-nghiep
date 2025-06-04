package com.dan.job_service.http_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.dan.job_service.configs.AuthenticationRequestInterceptor;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;

@FeignClient(name = "identity-service",
        url  = "${identity.service.url}",
        configuration = {AuthenticationRequestInterceptor.class}
)
public interface IdentityServiceClient {
    @GetMapping("/auth/user/{username}")
    UserDetailToCreateJob getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/auth/get/{userId}")
    UserDetailToCreateJob getUserById(@PathVariable("userId") String userId);
}
