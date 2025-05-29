package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobApplication;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job/jobs")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;
    private final JwtService jwtService;

    @PostMapping("/public/apply/{jobId}")
    public ResponseEntity<ResponseMessage> applyJob(
            @PathVariable String jobId,
            @RequestBody JobApplicationRequest jobApplicationRequest,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobApplicationService.applyJob(jobApplicationRequest, jobId, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Bạn vừa ứng tuyển công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi ứng tuyển công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/public/list-application/{jobId}")
    public ResponseEntity<Page<JobApplication>> getApplications(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        String username = jwtService.getUsernameFromRequest(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(jobApplicationService.getJobApplicationByJobId(jobId, username, pageable));
    }
}
