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
import com.dan.job_service.dtos.responses.JobApplicationApplied;
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

    @PostMapping(value = "/private/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<?> updateJob(@PathVariable String id, @Valid @ModelAttribute JobRequest jobRequest,
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

    // @PutMapping("/public/update/{id}")
    // public ResponseEntity<?> userUpdateJob(@PathVariable String id, @Valid @ModelAttribute JobRequest jobRequest,
    //         HttpServletRequest request) {
    //     try {
    //         String username = jwtService.getUsernameFromRequest(request);
    //         jobService.userUpdateJob(id, jobRequest, username);
    //         return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật công việc thành công"));
    //     } catch (Exception e) {
    //         log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
    //         return ResponseEntity.badRequest()
    //                 .body(new ResponseMessage(400, "Lỗi khi cập nhật công việc: " + e.getMessage()));
    //     }
    // }

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

    @GetMapping("/admin/get-all-jobs")
    public ResponseEntity<?> getAllJobsByAdmin(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String userId,

            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<JobDetail> jobsPage = jobService.getAll(categoryId, title, userId, pageable);
            return ResponseEntity.ok(jobsPage);
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc (admin): {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi lấy danh sách công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/public/get-all-jobs")
    public ResponseEntity<?> getAllJobsByUser(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String userId,
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
            Page<JobDetail> jobsPage = jobService.getAll(categoryId, title, userId, pageable);
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status)

    {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            Pageable pageable = PageRequest.of(page, size);
            Page<JobApplicationApplied> appliedJobsPage = jobService.getAppliedJobs(username, pageable, status);

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

    @PutMapping("/private/{jobId}/mark-done")
    public ResponseEntity<?> markJobAsDone(@PathVariable String jobId, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.markJobAsDone(jobId, username);
            log.info("Job {} đã được đánh dấu hoàn thành bởi người dùng: {}", jobId, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Đánh dấu công việc hoàn thành thành công"));
        } catch (Exception e) {
            log.error("Lỗi đánh dấu công việc hoàn thành ID {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi đánh dấu công việc hoàn thành: " + e.getMessage()));
        }
    }

    @PutMapping("/private/{jobId}/mark-undone")
    public ResponseEntity<?> markJobAsUndone(@PathVariable String jobId, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobService.markJobAsUndone(jobId, username);
            log.info("Job {} đã được hủy đánh dấu hoàn thành bởi người dùng: {}", jobId, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Hủy đánh dấu công việc hoàn thành thành công"));
        } catch (Exception e) {
            log.error("Lỗi hủy đánh dấu công việc hoàn thành ID {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi hủy đánh dấu công việc hoàn thành: " + e.getMessage()));
        }
    }

    @PutMapping("/private/update-status/{jobId}")
    public ResponseEntity<?> updateJobStatus(
            @PathVariable String jobId,
            @RequestParam Boolean status,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage response = jobService.updateJobStatus(jobId, status, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi cập nhật status công việc ID {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi cập nhật trạng thái công việc: " + e.getMessage()));
        }
    }

    @PutMapping("/private/update-active/{jobId}")
    public ResponseEntity<?> updateJobActive(
            @PathVariable String jobId,
            @RequestParam Boolean active,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            ResponseMessage response = jobService.updateJobActive(jobId, active, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi cập nhật active công việc ID {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi cập nhật trạng thái active: " + e.getMessage()));
        }
    }

    
}