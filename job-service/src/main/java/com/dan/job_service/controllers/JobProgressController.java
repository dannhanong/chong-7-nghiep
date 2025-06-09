package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.requets.UpdateStatusRequest;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job/jobs")
@RequiredArgsConstructor
public class JobProgressController {
    private final JobProgressService jobProgressService;
    private final JwtService jwtService;

    @PostMapping("/private/progress/{jobId}")
    public ResponseEntity<ResponseMessage> createJob(@PathVariable String jobId,
                                                     @RequestBody UpdateStatusRequest updateStatusRequest,
                                                     HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobProgressService.updateProgress(jobId, username, updateStatusRequest.status());
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật tiến độ công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi cập nhat tiến độ công việc: " + e.getMessage()));
        }
    }
}
