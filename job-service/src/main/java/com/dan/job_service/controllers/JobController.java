package com.dan.job_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/job/jobs")
public class JobController {
    @Autowired
    private JobService jobService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/private/create")
    public ResponseEntity<ResponseMessage> createJob(@Valid @RequestBody JobRequest jobRequest, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.create(jobRequest, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Tạo công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi tạo công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<?> getJobDetail(@PathVariable String id, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            JobDetail jobDetail = jobService.getJobById(id, username);
            return ResponseEntity.ok(jobDetail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi lấy thông tin công việc: " + e.getMessage()));
        }
    }
}
