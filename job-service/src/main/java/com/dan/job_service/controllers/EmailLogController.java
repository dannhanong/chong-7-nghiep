package com.dan.job_service.controllers;

import com.dan.job_service.models.EmailLog;
import com.dan.job_service.services.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/job/jobs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogService emailLogService;

    @PostMapping("/public/save-email")
    public ResponseEntity<EmailLog> saveEmailLog(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(emailLogService.saveEmailLog(request.get("username"), request.get("jobId")));
    }

    @PostMapping("/public/filter-jobs")
    public ResponseEntity<Map<String, List<Map<String, String>>>> filterUnsentJobs(
            @RequestBody Map<String, List<Map<String, String>>> request) {
        Map<String, List<Map<String, String>>> result = request.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String username = entry.getKey();
                            List<String> jobIds = entry.getValue().stream()
                                    .map(job -> job.get("_id"))
                                    .collect(Collectors.toList());
                            List<String> unsentJobIds = emailLogService.filterUnsentJobs(username, jobIds);
                            return unsentJobIds.stream()
                                    .map(jobId -> Map.of("jobId", jobId))
                                    .collect(Collectors.toList());
                        }
                ));
        return ResponseEntity.ok(result);
    }
} 