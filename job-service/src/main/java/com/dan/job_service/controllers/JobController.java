package com.dan.job_service.controllers;

import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import com.dan.job_service.models.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

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

    @GetMapping("/public/get-jobs-posted-last-24-hours")
    public ResponseEntity<?> getJobsPostedLast24Hours() {
        try {
            List<JobsLast24HoursResponse> jobList = jobService.getJobsPostedLast24Hours();
            return ResponseEntity.ok(jobList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @PutMapping("/public/update/{id}")
    public ResponseEntity<?> updateJob(@PathVariable String id, @Valid @RequestBody JobRequest jobRequest, HttpServletRequest request) {
        try{
            String username = jwtService.getUsernameFromRequest(request);
            jobService.update(id, jobRequest, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật công việc thành công"));
        }catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi xóa công việc: "+e.getMessage()));
        }
    }

    @DeleteMapping("/public/{id}")
    public ResponseEntity<?> deleteJobById(@PathVariable String id, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.delete(id, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Xóa công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi xóa công việc: " + e.getMessage()));
        }
    }
}
