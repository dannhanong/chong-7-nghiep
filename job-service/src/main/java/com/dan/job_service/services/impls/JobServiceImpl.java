package com.dan.job_service.services.impls;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dan.events.dtos.EventAddJobDataForRecommend;
import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.Category;
import com.dan.job_service.models.Job;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.DateFormatter;
import com.dan.job_service.services.JobService;

@Service
public class JobServiceImpl implements JobService{
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private IdentityServiceClient identityServiceClient;
    @Autowired
    private DateFormatter dateFormatter;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public ResponseMessage create(JobRequest jobRequest, String username) {
        Category category = categoryRepository
            .findById(jobRequest.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        UserDetailToCreateJob userDetailToCreateJob = identityServiceClient.getUserByUsername(username);
        
        String userId = userDetailToCreateJob.getId();

        // if (!userDetailToCreateJob.isIdentityVerified()) {
        //     throw new RuntimeException("Bạn cần xác minh danh tính trước khi đăng công việc");
        // }

        if (jobRequest.salaryMin() > jobRequest.salaryMax()) {
            throw new RuntimeException("Lương tối thiểu không được lớn hơn lương tối đa");
        }

        jobRepository.save(Job.builder()
            .userId(userId)
            .categoryId(category.getId())
            .title(jobRequest.title())
            .description(jobRequest.description())
            .salaryMin(jobRequest.salaryMin())
            .salaryMax(jobRequest.salaryMax())
            .experienceLevel(jobRequest.experienceLevel())
            .benefits(jobRequest.benefits())
            .applicationDeadline(jobRequest.applicationDeadline())
            .contentUri(jobRequest.contentUri())
            .active(true)
            .status(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
                
        return new ResponseMessage(200, "Tạo công việc thành công");
    }

    @Override
    @Transactional
    public ResponseMessage update(String id, JobRequest jobRequest, String username) {
         Job existingJob = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
         Category category = categoryRepository
             .findById(jobRequest.categoryId())
             .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        //  String userId = identityServiceClient.getUserByUsername(username).getId();

         if (jobRequest.salaryMin() > jobRequest.salaryMax()) {
             throw new RuntimeException("Lương tối thiểu không được lớn hơn lương tối đa");
         }

         existingJob.setCategoryId(category.getId());
         existingJob.setTitle(jobRequest.title());
         existingJob.setDescription(jobRequest.description());
         existingJob.setSalaryMin(jobRequest.salaryMin());
         existingJob.setSalaryMax(jobRequest.salaryMax());
         existingJob.setExperienceLevel(jobRequest.experienceLevel());
         existingJob.setBenefits(jobRequest.benefits());
         existingJob.setApplicationDeadline(jobRequest.applicationDeadline());
         existingJob.setContentUri(jobRequest.contentUri());
         existingJob.setUpdatedAt(java.time.LocalDateTime.now());
         jobRepository.save(existingJob);
        
        return new ResponseMessage(200, "Cập nhật công việc thành công");
    }

    @Override
    @Transactional
    public ResponseMessage delete(String id, String username) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
        UserDetailToCreateJob userDetailToCreateJob = identityServiceClient.getUserByUsername(username);
        String userId = userDetailToCreateJob.getId();

        if (!userId.equals(job.getUserId())) {
            throw new RuntimeException("Bạn không phải là người tạo công việc");
        }
        
        job.setActive(false);
        job.setDeletedAt(LocalDateTime.now());
        
        jobRepository.save(job);
        
        return new ResponseMessage(200, "Xóa công việc thành công");    
    }

    @Override
    public JobDetail getJobById(String id, String username) {
        Job job = jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
    
        if (username != null) {
            kafkaTemplate.send("job_get_job_by_id", EventAddJobDataForRecommend.builder()
                .userId(identityServiceClient.getUserByUsername(username).getId())
                .jobId(job.getId())
                .build());
        }
        return fromJobToJobDetail(job);
    }

    @Override
    public List<JobsLast24HoursResponse> getJobsPostedLast24Hours() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Job> jobs = jobRepository.findByCreatedAtBetweenAndActiveTrue(yesterday, today);
        return jobs.stream()
                .map(JobsLast24HoursResponse::fromJobToJobLast24Hours)
                .collect(Collectors.toList());
    }

    @Override
    public List<Job> getAll() {
        return jobRepository.findAll();
    }

    private JobDetail fromJobToJobDetail(Job job) {
        String userName = null;
        if (job.getUserId() != null) {
            UserDetailToCreateJob user = identityServiceClient.getUserById(job.getUserId());
            userName = user != null ? user.getName() : "Không xác định";
        }
        return JobDetail.builder()
            .id(job.getId())
            .userName(userName)
            .title(job.getTitle())
            .description(job.getDescription())
            .salaryMin(job.getSalaryMin())
            .salaryMax(job.getSalaryMax())
            .experienceLevel(job.getExperienceLevel())
            .benefits(job.getBenefits())
            .applicationDeadline(job.getApplicationDeadline())
            .status(job.getStatus())
            .active(job.getActive())
            .createdAt(dateFormatter.formatDate(job.getCreatedAt()))
            .updatedAt(dateFormatter.formatDate(job.getUpdatedAt()))
            .contentUri(job.getContentUri())
            .build();
    }
}
