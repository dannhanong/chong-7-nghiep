package com.dan.job_service.http_clients;

import com.dan.job_service.configs.AuthenticationRequestInterceptor;
import com.dan.job_service.dtos.responses.UserProfileDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileServiceClient {
    @GetMapping("/profile/detail")
    UserProfileDetailResponse getUserProfileDetail(HttpServletRequest request);

    @GetMapping("profile/public/get-detail/{username}")
    UserProfileDetailResponse getUserProfileByUsername(@PathVariable("username") String username);

    @GetMapping("profile/get/{userId}")
    UserProfileDetailResponse getProfileByUserId(@PathVariable("userId") String userId);
}
