package com.dan.job_service.services.impls;

import java.time.LocalDateTime;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

        kafkaTemplate.send("job_minus_job_count", userId);
                
        return new ResponseMessage(200, "Tạo công việc thành công");
    }

    @Override
    public ResponseMessage update(String id, JobRequest jobRequest, String username) {
        // Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
        // Category category = categoryRepository
        //     .findById(jobRequest.getCategoryId())
        //     .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        // String userId = identityServiceClient.getUserByUsername(username).get("id");
        // Company company = companyRepository
        //     .findByUserId(userId)
        //     .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty"));
        // if (userId == null) {
        //     throw new RuntimeException("Không tìm thấy người dùng");
        // }
        // if (!company.getUserId().equals(userId)) {
        //     throw new RuntimeException("Bạn không phải là người tạo công ty");
        // }
        // if (jobRequest.getSalaryMin() > jobRequest.getSalaryMax()) {
        //     throw new RuntimeException("Lương tối thiểu không được lớn hơn lương tối đa");
        // }
        
        // job.setCategoryId(category.getId());
        // job.setTitle(jobRequest.getTitle());
        // job.setDescription(jobRequest.getDescription());
        // job.setSalaryMin(jobRequest.getSalaryMin());
        // job.setSalaryMax(jobRequest.getSalaryMax());
        // job.setExperienceLevel(jobRequest.getExperienceLevel());
        // job.setBenefits(jobRequest.getBenefits());
        // job.setApplicationDeadline(jobRequest.getApplicationDeadline());
        // job.setContentUri(jobRequest.getContentUri());
        // job.setUpdatedAt(java.time.LocalDateTime.now());

        // jobRepository.save(job);
        
        return new ResponseMessage(200, "Cập nhật công việc thành công");
    }

    @Override
    public ResponseMessage delete(String id, String username) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
        UserDetailToCreateJob userDetailToCreateJob = identityServiceClient.getUserByUsername(username);
        String userId = userDetailToCreateJob.getId();

        if (!userId.equals(job.getUserId())) {
            throw new RuntimeException("Bạn không phải là người tạo công việc");
        }
        
        job.setActive(false);
        job.setDeletedAt(java.time.LocalDateTime.now());
        
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
    
    private JobDetail fromJobToJobDetail(Job job) {
        String userName = null;
        if (job.getUserId() != null) {
            UserDetailToCreateJob user = identityServiceClient.getUserById(job.getUserId());
            userName = user != null ? user.getName() : null;
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
