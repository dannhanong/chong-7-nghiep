package com.dan.job_service.services.impls;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dan.events.dtos.EventAddJobDataForRecommend;
import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.Category;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.User;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.repositories.UserRepository;
import com.dan.job_service.services.DateFormatter;
import com.dan.job_service.services.JobService;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository; // Thêm repository cho User
    @Autowired
    private DateFormatter dateFormatter;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public ResponseMessage create(JobRequest jobRequest, String username) {
        try {
            Category category = categoryRepository
                .findById(jobRequest.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

            User user = userRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với username: " + username));
            String userId = user.getId();

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
        } catch (Exception e) {
            log.error("Lỗi tạo công việc cho username {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseMessage update(String id, JobRequest jobRequest, String username) {
        try {
            Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
            Category category = categoryRepository
                .findById(jobRequest.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

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
            existingJob.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(existingJob);

            return new ResponseMessage(200, "Cập nhật công việc thành công");
        } catch (Exception e) {
            log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseMessage delete(String id, String username) {
        try {
            Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
            User user = userRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với username: " + username));
            String userId = user.getId();

            if (!userId.equals(job.getUserId())) {
                throw new RuntimeException("Bạn không phải là người tạo công việc");
            }

            job.setActive(false);
            job.setDeletedAt(LocalDateTime.now());
            jobRepository.save(job);

            return new ResponseMessage(200, "Xóa công việc thành công");
        } catch (Exception e) {
            log.error("Lỗi xóa công việc ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public JobDetail getJobById(String id, String username) {
        try {
            Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

            if (username != null) {
                User user = userRepository.findById(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với username: " + username));
                kafkaTemplate.send("job_get_job_by_id", EventAddJobDataForRecommend.builder()
                    .userId(user.getId())
                    .jobId(job.getId())
                    .build());
            }
            return fromJobToJobDetail(job);
        } catch (Exception e) {
            log.error("Lỗi lấy chi tiết công việc ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<JobsLast24HoursResponse> getJobsPostedLast24Hours() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

            List<Job> jobs = jobRepository.findByCreatedAtBetweenAndActiveTrue(yesterday, today);
            return jobs.stream()
                .map(JobsLast24HoursResponse::fromJobToJobLast24Hours)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc 24h: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<JobDetail> getAll(String categoryId, String title, Pageable pageable) {
        try {
            log.info("Lấy danh sách công việc với categoryId: {}, title: {}, pageable: {}", categoryId, title, pageable);
            Page<Job> jobsPage;

            if (categoryId != null && !categoryId.isEmpty()) {
                categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
            }

            if (categoryId != null && !categoryId.isEmpty() && title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByCategoryIdAndTitleContainingIgnoreCaseAndActiveTrue(categoryId, title, pageable);
            } else if (categoryId != null && !categoryId.isEmpty()) {
                jobsPage = jobRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
            } else if (title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByTitleContainingIgnoreCaseAndActiveTrue(title, pageable);
            } else {
                jobsPage = jobRepository.findByActiveTrue(pageable);
            }

            List<JobDetail> jobDetails = jobsPage.getContent().stream()
                .map(this::fromJobToJobDetail)
                .collect(Collectors.toList());

            log.info("Số lượng công việc tìm thấy: {}", jobsPage.getTotalElements());
            return new PageImpl<>(jobDetails, pageable, jobsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi lấy danh sách công việc: {}", e.getMessage(), e);
            throw e;
        }
    }

    private JobDetail fromJobToJobDetail(Job job) {
        String userName = "Không xác định";
        if (job.getUserId() != null) {
            try {
                log.info("Đang tìm người dùng với userId: {}", job.getUserId());
                User user = userRepository.findById(job.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với userId: " + job.getUserId()));
                if (user.getName() != null && !user.getName().isEmpty()) {
                    userName = user.getUsername();
                    log.info("userName được đặt thành: {}", userName);
                } else {
                    log.warn("Tên người dùng trống cho userId: {}", job.getUserId());
                }
            } catch (Exception e) {
                log.error("Lỗi khi lấy thông tin người dùng cho userId {}: {}", job.getUserId(), e.getMessage(), e);
            }
        } else {
            log.warn("userId của công việc {} là null", job.getId());
        }

        String categoryName = "Không xác định";
        if (job.getCategoryId() != null) {
            try {
                Category category = categoryRepository.findById(job.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
                categoryName = category.getName();
            } catch (Exception e) {
                log.warn("Không thể lấy thông tin danh mục cho categoryId {}: {}", job.getCategoryId(), e.getMessage());
            }
        }

        return JobDetail.builder()
            .id(job.getId())
            .userName(userName)
            .categoryName(categoryName)
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