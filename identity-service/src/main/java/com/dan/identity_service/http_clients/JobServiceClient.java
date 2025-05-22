package com.dan.identity_service.http_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.dan.identity_service.configs.AuthenticationRequestInterceptor;

@FeignClient(name = "job-service", configuration = {AuthenticationRequestInterceptor.class})
public interface JobServiceClient {
    // @PostMapping(value = "/job/companies/public/get/{companyId}")
}
