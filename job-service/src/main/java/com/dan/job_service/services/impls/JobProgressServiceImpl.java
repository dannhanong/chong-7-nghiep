package com.dan.job_service.services.impls;

import com.dan.job_service.controllers.JobApplicationController;
import com.dan.job_service.dtos.enums.JobStatus;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.JobProgress;
import com.dan.job_service.repositories.JobProgressRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.JobProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.*;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class JobProgressServiceImpl implements JobProgressService {
    private final JobProgressRepository jobProgressRepository;
    private final JobRepository jobRepository;
    private final IdentityServiceClient identityServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);

    @Override
    public ResponseMessage updateProgress(String jobId, String username, String status) {
        try {
            jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với id: " + jobId));
            String userId = identityServiceClient.getUserByUsername(username).getId();

            JobProgress newJobProgress = JobProgress.builder()
                    .jobId(jobId)
                    .userId(userId)
                    .status(JobStatus.valueOf(status))
                    .createdAt(LocalDateTime.now()).build();
            jobProgressRepository.save(newJobProgress);
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với id: " + jobId));
            
            logger.info("trang thai", JobStatus.valueOf(status));
            if (JobStatus.valueOf(status) == JobStatus.COMPLETED || JobStatus.valueOf(status) == JobStatus.CANCELED) {
                job.setDone(true);
                jobRepository.save(job);
                logger.info("trang thai cap nhat thanh", "true");
            }else{
                 job.setDone(false);
                jobRepository.save(job);
            }
            return ResponseMessage.builder()
                    .status(200)
                    .message("Cập nhật tiến độ thành công")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật tiến độ thất bại: " + e.getMessage());
        }
    }

    @Override
    public JobProgress getLatestProgress(String jobId) {
        return jobProgressRepository.findByJobId(jobId)
                .stream()
                .max(Comparator.comparing(JobProgress::getCreatedAt))
                .orElse(null);
    }
}