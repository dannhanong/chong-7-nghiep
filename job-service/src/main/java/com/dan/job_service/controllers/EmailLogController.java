package com.dan.job_service.controllers;

import com.dan.events.dtos.responses.RecommendJobGmailResponse;
import com.dan.job_service.models.EmailLog;
import com.dan.job_service.services.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/job/jobs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogService emailLogService;

    @PostMapping("/public/save-email")
    public ResponseEntity<EmailLog> saveEmailLog(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(emailLogService.saveEmailLog(request.get("email"), request.get("jobId")));
    }

    // @PostMapping("/public/filter-jobs")
    // public ResponseEntity<Map<String, List<String>>> filterUnsentJobs(
    //         @RequestBody Map<String, List<Map<String, String>>> request) {
    //     return ResponseEntity.ok(emailLogService.filterUnsentJobs(request));
    // }
    
    @GetMapping("/public/gmail-jobs")
    public ResponseEntity<List<RecommendJobGmailResponse>> getGmailJobs() {
        return ResponseEntity.ok(emailLogService.getGmailJobs());
    }
}