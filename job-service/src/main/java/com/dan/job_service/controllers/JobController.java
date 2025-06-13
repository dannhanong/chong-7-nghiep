package com.dan.job_service.controllers;

import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;
    @Autowired
    private JwtService jwtService;

    @PostMapping(
            value = "/private/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage> createJob(
            @Valid @ModelAttribute JobRequest jobRequest,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.create(jobRequest, username);
            log.info("Công việc đã được tạo thành công bởi người dùng: {}", username);
            return ResponseEntity.ok(new ResponseMessage(200, "Tạo công việc thành công"));
        } catch (Exception e) {
            log.error("Lỗi tạo công việc: {}", e.getMessage(), e);
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
            log.error("Lỗi lấy chi tiết công việc ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi lấy thông tin công việc: " + e.getMessage()));
        }
    }



    @GetMapping("/public/get-jobs-posted-last-24-hours")
    public ResponseEntity<?> getJobsPostedLast24Hours() {
        try {
            List<JobsLast24HoursResponse> jobList = jobService.getJobsPostedLast24Hours();
            return ResponseEntity.ok(jobList);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc 24h: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @PutMapping("/private/update/{id}")
    public ResponseEntity<?> updateJob(@PathVariable String id, @Valid @RequestBody JobRequest jobRequest,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.update(id, jobRequest, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật công việc thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi cập nhật công việc: " + e.getMessage()));
        }
    }

    @PutMapping("/public/update/{id}")
    public ResponseEntity<?> userUpdateJob(@PathVariable String id, @Valid @RequestBody JobRequest jobRequest,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.userUpdateJob(id, jobRequest, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật công việc thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi cập nhật công việc: " + e.getMessage()));
        }
    }

    @DeleteMapping("/private/{id}")
    public ResponseEntity<?> deleteJobById(@PathVariable String id, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);

            
            jobService.delete(id, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Xóa công việc thành công"));
        } catch (Exception e) {
            log.error("Lỗi xóa công việc ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi xóa công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/private/get-all-jobs")
    public ResponseEntity<?> getAllJobsByAdmin(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<JobDetail> jobsPage = jobService.getAll(categoryId, title, pageable);
            return ResponseEntity.ok(jobsPage);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc (admin): {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/public/get-all-jobs")
    public ResponseEntity<?> getAllJobs(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (page < 0 || size <= 0) {
                log.warn("Tham số không hợp lệ: page={}, size={}", page, size);
                return ResponseEntity.badRequest()
                        .body(new ResponseMessage(400, "Tham số page hoặc size không hợp lệ"));
            }
            log.info("Lấy danh sách công việc với categoryId: {}, title: {}, page: {}, size: {}", categoryId, title,
                    page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<JobDetail> jobsPage = jobService.getAll(categoryId, title, pageable);
            log.info("Số lượng công việc tìm thấy: {}", jobsPage.getTotalElements());
            if (jobsPage.isEmpty()) {
                return ResponseEntity.ok(new ResponseMessage(200, "Không có công việc nào phù hợp"));
            }
            return ResponseEntity.ok(jobsPage);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc: categoryId={}, title={}, page={}, size={}: {}",
                    categoryId, title, page, size, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/private/get-jobs-by-user")
    public ResponseEntity<?> getAllJobsByUser(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<JobDetail> jobsPage = jobService.getJobsByUserId(username, pageable);
            return ResponseEntity.ok(jobsPage);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc (admin): {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/private/get-applied-jobs")
    public ResponseEntity<?> getAppliedJobs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<JobDetail> appliedJobsPage = jobService.getAppliedJobs(username, pageable);
            
            if (appliedJobsPage.isEmpty()) {
                return ResponseEntity.ok(new ResponseMessage(200, "Bạn chưa ứng tuyển công việc nào"));
            }
            
            return ResponseEntity.ok(appliedJobsPage);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc đã ứng tuyển: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc đã ứng tuyển: " + e.getMessage()));
        }
    }
    
}