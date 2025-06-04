package com.dan.job_service.http_clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.dan.events.dtos.responses.RecommendJobGmailResponse;

@FeignClient(name = "recommend-service")
public interface RecommendClient {
    @GetMapping("/recommend/gmail-jobs")
    List<RecommendJobGmailResponse> getGmailJobs();
}
