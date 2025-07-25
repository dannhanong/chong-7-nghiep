package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.requets.UpdateStatusRequest;
import com.dan.job_service.dtos.responses.JobApplicationProfileResponse;
import com.dan.job_service.dtos.responses.JobApplicationResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.repositories.UserInteractionRepository;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;

import org.springframework.data.domain.Page;
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
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);

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
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi khi ứng tuyển công việc: " + e.getMessage()));
        }
    }

    // Lấy danh sách đơn ứng tuyển của người dùng public Page<JobApplication>
    // getJobApplicationByUserId(String userId, String username, Pageable pageable)
    // {
    @GetMapping("/private/list-application")
    public ResponseEntity<?> getApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ResponseMessage(400, "Không tìm thấy người dùng"));
            }
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(jobApplicationService.getJobApplicationByUserId(username, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Không tìm thấy người dùng"));
        }
    }

    @GetMapping("/private/list-application/{jobId}")
    public ResponseEntity<Page<JobApplicationResponse>> getApplications(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        String username = jwtService.getUsernameFromRequest(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(jobApplicationService.getJobApplicationByJobId(jobId, username, pageable));
    }
    @GetMapping("/private/application/{jobId}")
    public ResponseEntity<JobApplicationResponse> getApplicationsById(
            @PathVariable String jobId,
            HttpServletRequest request) {
        String username = jwtService.getUsernameFromRequest(request);
        return ResponseEntity.ok(jobApplicationService.getJobApplicationByJobId(jobId,username));
    }

    @GetMapping("/public/list-application/{jobId}")
    public ResponseEntity<Page<JobApplicationProfileResponse>> getPublicApplicationsById(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
          ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(jobApplicationService.getPublicJobApplicationByJobId(jobId, pageable));
    }

    @PutMapping("/private/status/{id}")
    public ResponseEntity<ResponseMessage> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest updateStatusRequest) {
        try {
            jobApplicationService.updateStatus(id, updateStatusRequest.status());
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật trạng thái đơn ứng tuyển thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ResponseMessage(400, "Lỗi khi cập nhật trạng thái đơn ứng tuyển: " + e.getMessage()));
        }
    }

    @GetMapping("/public/count-applied/{userId}")
    public Long countApplied(@PathVariable String userId) {
        return jobApplicationService.countAppliedSuccess(userId);
    }

    @GetMapping("/applications/{applicationId}/detail")
    public ResponseEntity<?> getJobApplicationDetail(@PathVariable String applicationId) {
        try {
            return ResponseEntity.ok(jobApplicationService.getJobApplicationDetail(applicationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi lấy thông tin đơn ứng tuyển: " + e.getMessage()));
        }
    }

    @DeleteMapping("/private/delete/{applicationId}")
    public ResponseEntity<ResponseMessage> deleteJobApplication(@PathVariable String id) {
        return ResponseEntity.ok(jobApplicationService.delete(id));
    }
}
