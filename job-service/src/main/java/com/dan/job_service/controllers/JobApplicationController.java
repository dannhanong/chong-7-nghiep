package com.dan.job_service.controllers;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.requets.UpdateStatusRequest;
import com.dan.job_service.dtos.responses.JobApplicationResponse;
import com.dan.job_service.dtos.responses.JobApplicationWithJobResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.JobApplication;
import com.dan.job_service.repositories.UserInteractionRepository;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job/jobs")
@RequiredArgsConstructor
public class JobApplicationController {
    private final JobApplicationService jobApplicationService;
    private final JwtService jwtService;
    private final UserInteractionRepository userInteractionRepository;

    @PostMapping("/private/apply/{jobId}")
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

    // Lấy danh sách đơn ứng tuyển của người dùng     public Page<JobApplication> getJobApplicationByUserId(String userId, String username, Pageable pageable) {
@GetMapping("/private/list-application")
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0));
            }
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(jobApplicationService.getJobApplicationByUserId(username, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0));
        }
    }

    @GetMapping("/private/list-application/{jobId}")
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        String username = jwtService.getUsernameFromRequest(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(jobApplicationService.getJobApplicationByJobId(jobId, username, pageable));
    }

    @PutMapping("/private/status/{id}")
    public ResponseEntity<ResponseMessage> updateStatus(
            @PathVariable String id, 
            @RequestBody UpdateStatusRequest updateStatusRequest) {
        try {
            jobApplicationService.updateStatus(id, updateStatusRequest.status());
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật trạng thái đơn ứng tuyển thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ResponseMessage(400, "Lỗi khi cập nhật trạng thái đơn ứng tuyển: " + e.getMessage()));
        }
    }
}
