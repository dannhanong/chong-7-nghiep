package com.dan.identity_service.http_clients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dan.identity_service.dtos.responses.Oauth2UserResponse;

@FeignClient(name = "oauth2-user-client", url = "https://www.googleapis.com")
public interface Oauth2UserClient {
    @GetMapping(value = "/oauth2/v1/userinfo")
    Oauth2UserResponse getUserInfo(@RequestParam("alt") String alt,
                                   @RequestParam("access_token") String accessToken);
}
