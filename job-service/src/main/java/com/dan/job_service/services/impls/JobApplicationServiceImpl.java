package com.dan.job_service.services.impls;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.JobApplication;
import com.dan.job_service.repositories.JobApplicationRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final IdentityServiceClient identityServiceClient;

    @Override
    public ResponseMessage applyJob(JobApplicationRequest request, String jobId, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        if (jobApplicationRepository.findByUserIdAndJobId(userId, jobId).isPresent()) {
            throw new RuntimeException("Bạn đã ứng tuyển công việc này");
        }
        if(request.offerSalary() < job.getSalaryMin() || request.offerSalary() > job.getSalaryMax()){
            throw new RuntimeException("Lương đề xuất phải nằm trong khoảng từ " + job.getSalaryMin() + " đến " + job.getSalaryMax());
        }

        JobApplication application = JobApplication.builder()
            .userId(userId)
            .jobId(jobId)
            .status(ApplicationStatus.PENDING)
            .offerSalary(request.offerSalary())
            .offerPlan(request.offerPlan())
            .offerSkill(request.offerSkill())
            .appliedAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
            
        jobApplicationRepository.save(application);
        
        return ResponseMessage.builder()
            .status(200)
            .message("Nộp đơn ứng tuyển thành công")
            .build();
    }

    @Override
    public Page<JobApplication> getJobApplicationByUserId(String userId, String username, Pageable pageable) {
        return null;
    }

    @Override
    public Page<JobApplication> getJobApplicationByJobId(String jobId, String username, Pageable pageable) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        if (!job.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem đơn ứng tuyển");
        }

        return jobApplicationRepository.findByJobId(jobId, pageable);
    }
}
