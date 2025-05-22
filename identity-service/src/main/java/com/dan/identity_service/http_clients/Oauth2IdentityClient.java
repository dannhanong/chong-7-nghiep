package com.dan.identity_service.http_clients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import com.dan.identity_service.dtos.requests.ExchangeTokenRequest;
import com.dan.identity_service.dtos.responses.ExchangeTokenResponse;

import feign.QueryMap;

@FeignClient(name = "oauth2-identity", url = "https://oauth2.googleapis.com")
public interface Oauth2IdentityClient {
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest exchangeTokenRequest);
}
