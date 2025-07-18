package com.dan.job_service.services.impls;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import com.dan.job_service.http_clients.FileServiceClient;
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
import com.dan.events.dtos.JobEvent;
import com.dan.events.dtos.RecentActivityJobMessage;
import com.dan.events.dtos.RecentActivityRevenueMessage;
import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.JobApplicationApplied;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.Category;
import com.dan.job_service.models.Job;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.repositories.JobProgressRepository;
import com.dan.job_service.services.DateFormatter;
import com.dan.job_service.services.JobService;
import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.enums.JobStatus;
import com.dan.job_service.models.JobProgress;
import com.dan.job_service.repositories.JobApplicationRepository;
import com.dan.job_service.repositories.JobEmbeddingRepository;
import com.dan.job_service.models.JobApplication;
import com.dan.job_service.models.JobEmbedding;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private DateFormatter dateFormatter;
    @Autowired
    private IdentityServiceClient identityServiceClient;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private JobProgressRepository jobProgressRepository;
    @Autowired
    private JobApplicationRepository jobApplicationRepository;
    @Autowired
    private JobEmbeddingRepository jobEmbeddingRepository;
    @Autowired
    private FileServiceClient fileServiceClient;

    @Override
    @Transactional
    public ResponseMessage create(JobRequest jobRequest, String username) {
        try {
            Category category = categoryRepository
                    .findById(jobRequest.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            String userId = user.getId();

            if (jobRequest.salaryMin() > jobRequest.salaryMax()) {
                throw new RuntimeException("Lương tối thiểu không được lớn hơn lương tối đa");
            }

            Job newJob = Job.builder()
                    .userId(userId)
                    .categoryId(category.getId())
                    .title(jobRequest.title())
                    .shortDescription(jobRequest.shortDescription())
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
                    .tags(jobRequest.tags() != null ? jobRequest.tags() : List.of()) // Thêm trường tags
                    .workingType(jobRequest.workingType())
                    .workingForm(jobRequest.workingForm())
                    .build();

            MultipartFile file = jobRequest.file();
            if (file != null && !file.isEmpty()) {
                Map<String, String> res = fileServiceClient.uploadFile(file);
                String fileCode = res.get("fileCode");
                newJob.setFile(fileCode);
            }

            if (jobRequest.otherFiles() != null && !jobRequest.otherFiles().isEmpty()) {
                List<MultipartFile> otherFiles = jobRequest.otherFiles();
                List<String> fileCodes = fileServiceClient.uploadMultipleFilesForJob(otherFiles);
                newJob.setOtherImageCodes(fileCodes);
            }

            Job savedJob = jobRepository.save(newJob);

            // Tạo tiến dộ công việc
            JobProgress initialProgress = JobProgress.builder()
                    .jobId(savedJob.getId()).userId(userId)
                    .status(JobStatus.SEARCHING)
                    .createdAt(LocalDateTime.now())
                    .build();
            jobProgressRepository.save(initialProgress);

            JobEmbedding jobEmbedding = JobEmbedding.builder()
                    .jobId(savedJob.getId())
                    .embedding(List.of()) // Giả sử bạn sẽ cập nhật embedding sau
                    .updatedAt(LocalDateTime.now())
                    .build();

            jobEmbeddingRepository.save(jobEmbedding);

            JobEvent jobEvent = JobEvent.builder()
                    .eventType("CREATE")
                    .data(savedJob)
                    .build();
            kafkaTemplate.send("job_created", jobEvent);

            kafkaTemplate.send("create_recent_activity_job", RecentActivityJobMessage.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .jobId(savedJob.getId())
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
            existingJob.setSalaryMin(jobRequest.salaryMin());
            existingJob.setSalaryMax(jobRequest.salaryMax());
            existingJob.setExperienceLevel(jobRequest.experienceLevel());
            existingJob.setBenefits(jobRequest.benefits());
            existingJob.setApplicationDeadline(jobRequest.applicationDeadline());
            existingJob.setContentUri(jobRequest.contentUri());
            existingJob.setUpdatedAt(LocalDateTime.now());
            existingJob.setWorkingType(jobRequest.workingType());
            existingJob.setWorkingForm(jobRequest.workingForm());
            existingJob.setStatus(jobRequest.status() != null ? jobRequest.status() : existingJob.getStatus());
            existingJob.setActive(jobRequest.active() != null ? jobRequest.active() : existingJob.getActive());
            existingJob.setTags(jobRequest.tags() != null ? jobRequest.tags() : existingJob.getTags());
            existingJob.setDone(jobRequest.done() != null ? jobRequest.done() : existingJob.getDone());
            
            boolean descriptionChanged = !jobRequest.description().equals(existingJob.getDescription()) && 
                jobRequest.description() != null && 
                !jobRequest.description().isEmpty();
            if (descriptionChanged) {
                existingJob.setDescription(jobRequest.description());
            } else {
                existingJob.setDescription(existingJob.getDescription());
            }

            MultipartFile file = jobRequest.file();
            if (file != null && !file.isEmpty()) {
                String existingFileCode = existingJob.getFile();
                Map<String, String> res = fileServiceClient.uploadFile(file);
                String fileCode = res.get("fileCode");
                existingJob.setFile(fileCode);
                if (existingFileCode != null && !existingFileCode.isEmpty()) {
                    kafkaTemplate.send("delete-file-by-fileCode", existingFileCode);
                }
            }
            if (jobRequest.otherFiles() != null && !jobRequest.otherFiles().isEmpty()) {
                List<String> existingOtherImageCodes = existingJob.getOtherImageCodes();
                if (existingOtherImageCodes != null && !existingOtherImageCodes.isEmpty()) {
                    kafkaTemplate.send("delete-file-by-fileCodes", existingOtherImageCodes);
                }

                List<MultipartFile> otherFiles = jobRequest.otherFiles();
                List<String> fileCodes = fileServiceClient.uploadMultipleFilesForJob(otherFiles);
                existingJob.setOtherImageCodes(fileCodes);
            }

            jobRepository.save(existingJob);

            JobEvent jobEvent = JobEvent.builder()
                    .eventType("UPDATE")
                    .data(existingJob)
                    .build();
            
            if (descriptionChanged) {
                kafkaTemplate.send("job_updated", jobEvent);
            } else {
                kafkaTemplate.send("job_updated_without_description_change", jobEvent);
            }

            return new ResponseMessage(200, "Cập nhật công việc thành công");
        } catch (Exception e) {
            log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseMessage userUpdateJob(String id, JobRequest jobRequest, String username) {
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
            existingJob.setShortDescription(jobRequest.shortDescription());
            existingJob.setDescription(jobRequest.description());
            existingJob.setSalaryMin(jobRequest.salaryMin());
            existingJob.setSalaryMax(jobRequest.salaryMax());
            existingJob.setExperienceLevel(jobRequest.experienceLevel());
            existingJob.setBenefits(jobRequest.benefits());
            existingJob.setApplicationDeadline(jobRequest.applicationDeadline());
            existingJob.setContentUri(jobRequest.contentUri());
            existingJob.setUpdatedAt(LocalDateTime.now());
            existingJob.setWorkingType(jobRequest.workingType());
            existingJob.setWorkingForm(jobRequest.workingForm());
            existingJob.setActive(jobRequest.active() != null ? jobRequest.active() : existingJob.getActive());
            existingJob.setDone(jobRequest.done() != null ? jobRequest.done() : existingJob.getDone());
            existingJob.setTags(jobRequest.tags() != null ? jobRequest.tags() : existingJob.getTags());
            jobRepository.save(existingJob);

            return new ResponseMessage(200, "Cập nhật công việc thành công");
        } catch (Exception e) {
            log.error("Lỗi cập nhật công việc ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<Job> getJobsCategoryId(String categoryId, Pageable pageable) {
        if (categoryId != null && !categoryId.isEmpty()) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
            return jobRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        } else {
            return jobRepository.findByActiveTrue(pageable);
        }
    }

    @Override
    @Transactional
    public ResponseMessage delete(String id, String username) {
        try {
            Job job = jobRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("Không tìm thấy người dùng");
            }
            String userId = user.getId();

            System.out.println("User ID: " + userId);

            if (!userId.equals(job.getUserId())) {
                throw new RuntimeException("Bạn không phải là người tạo công việc");
            }

            job.setActive(false);
            job.setDeletedAt(LocalDateTime.now());
            jobRepository.save(job);

            JobEvent jobEvent = JobEvent.builder()
                    .eventType("DELETE")
                    .data(job)
                    .build();

            kafkaTemplate.send("job_deleted", jobEvent);

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
                UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
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
public Page<JobDetail> getAll(String categoryId, String title, String userId, Pageable pageable) {
    try {
        log.info("Lấy danh sách công việc với categoryId: {}, title: {}, userId: {}, pageable: {}", 
                categoryId, title, userId, pageable);
        Page<Job> jobsPage;

        // Kiểm tra categoryId có tồn tại không
        if (categoryId != null && !categoryId.isEmpty()) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        }

        // Kết hợp các điều kiện lọc
        if (userId != null && !userId.isEmpty()) {
            if (categoryId != null && !categoryId.isEmpty() && title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByUserIdAndCategoryIdAndTitleContainingIgnoreCaseAndActiveTrue(
                        userId, categoryId, title, pageable);
            } else if (categoryId != null && !categoryId.isEmpty()) {
                jobsPage = jobRepository.findByUserIdAndCategoryIdAndActiveTrue(userId, categoryId, pageable);
            } else if (title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByUserIdAndTitleContainingIgnoreCaseAndActiveTrue(userId, title, pageable);
            } else {
                jobsPage = jobRepository.findJobsByUserIdAndActiveTrue(userId, pageable);
            }
        } else {
            if (categoryId != null && !categoryId.isEmpty() && title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByCategoryIdAndTitleContainingIgnoreCaseAndActiveTrue(categoryId, title, pageable);
            } else if (categoryId != null && !categoryId.isEmpty()) {
                jobsPage = jobRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
            } else if (title != null && !title.isEmpty()) {
                jobsPage = jobRepository.findByTitleContainingIgnoreCaseAndActiveTrue(title, pageable);
            } else {
                jobsPage = jobRepository.findByActiveTrue(pageable);
            }
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

    @Override
    public Page<JobDetail> getJobsByUserId(String username, Pageable pageable) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Page<Job> jobsPage = jobRepository.findJobsByUserIdAndActiveTrue(userId, pageable);
        List<JobDetail> jobDetails = jobsPage.getContent().stream()
                .map(this::fromJobToJobDetail)
                .collect(Collectors.toList());

        return new PageImpl<>(jobDetails, pageable, jobsPage.getTotalElements());
    }

@Override
public Page<JobApplicationApplied> getAppliedJobs(String username, Pageable pageable, String status) {
    try {
        UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
        String userId = user.getId();

        Page<JobApplication> userApplications;
        if (status != null && !status.isEmpty()) {
            userApplications = jobApplicationRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            userApplications = jobApplicationRepository.findByUserId(userId, pageable);
        }

        List<JobApplicationApplied> appliedJobs = userApplications.getContent()
            .stream()
            .map(application -> {
                Job job = jobRepository.findById(application.getJobId()).orElse(null);
                if (job == null) return null;
                return JobApplicationApplied.builder()
                        .id(job.getId())
                        .title(job.getTitle())
                        .shortDescription(job.getShortDescription())
                        .salaryMin(job.getSalaryMin())
                        .salaryMax(job.getSalaryMax())
                        .applicationDeadline(job.getApplicationDeadline())
                        .done(job.getDone())
                        .status(application.getStatus() != null ? application.getStatus().toString() : null)
                        .build();
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());

        return new PageImpl<>(appliedJobs, pageable, userApplications.getTotalElements());
    } catch (Exception e) {
        log.error("Error getting applied jobs for user {}: {}", username, e.getMessage(), e);
        throw e;
    }
}


    private JobDetail fromJobToJobDetail(Job job) {
        String userName = "Không xác định";

        String name = "Không xác định"; // Khởi tạo name
        Integer sumJob = 0; // Khởi tạo sumJob
        if (job.getUserId() != null) {
            try {
                log.info("Đang tìm người dùng với userId: {}", job.getUserId());
                UserDetailToCreateJob user = identityServiceClient.getUserById(job.getUserId());
                if (user.getName() != null && !user.getName().isEmpty()) {
                    userName = user.getUsername();
                    name = user.getName(); // Cập nhật name từ user
                    log.info("userName được đặt thành: {}", userName);
                } else {
                    log.warn("Tên người dùng trống cho userId: {}", job.getUserId());
                }
                // Đếm số job của userId này
                sumJob = jobRepository.countByUserIdAndActiveTrue(job.getUserId());
                log.info("Số lượng job của userId {}: {}", job.getUserId(), sumJob);
            } catch (Exception e) {
                log.error("Lỗi khi lấy thông tin người dùng hoặc đếm job cho userId {}: {}", job.getUserId(),
                        e.getMessage(), e);
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
                .name(name)
                .categoryName(categoryName)
                .categoryId(job.getCategoryId())
                .userId(job.getUserId())
                .title(job.getTitle())
                .shortDescription(job.getShortDescription())
                .description(job.getDescription())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .experienceLevel(job.getExperienceLevel())
                .benefits(job.getBenefits())
                .applicationDeadline(job.getApplicationDeadline())
                .status(job.getStatus())
                .active(job.getActive())
                .file(job.getFile())
                .otherFiles(job.getOtherImageCodes()) // <-- DÒNG MÃ ĐÃ ĐƯỢC THÊM VÀO
                .tags(job.getTags()) // Thêm trường tags
                .createdAt(dateFormatter.formatDate(job.getCreatedAt()))
                .updatedAt(dateFormatter.formatDate(job.getUpdatedAt()))
                .contentUri(job.getContentUri())
                .workingType(job.getWorkingType())
                .workingForm(job.getWorkingForm())
                .sumJob(sumJob) // Gán số lượng job vào đây
                .build();
    }

    @Override
    @Transactional
    public ResponseMessage markJobAsDone(String jobId, String username) {
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            String userId = user.getId();

            // Kiểm tra quyền: chỉ chủ job mới có thể đánh dấu hoàn thành
            if (!userId.equals(job.getUserId())) {
                throw new RuntimeException("Bạn không phải là người tạo công việc này");
            }

            // Kiểm tra job đã active chưa
            if (job.getActive() == null || !job.getActive()) {
                throw new RuntimeException("Công việc chưa được kích hoạt");
            }

            job.setDone(true);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Job {} đã được đánh dấu hoàn thành bởi user {}", jobId, username);

            kafkaTemplate.send("create_recent_activity_job_done", RecentActivityJobMessage.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .jobId(job.getId())
                    .build());

            List<JobApplication> applications = jobApplicationRepository.findByJobIdAndStatus(jobId, ApplicationStatus.APPROVED);

            applications.forEach(application -> {
                UserDetailToCreateJob applicationUser = identityServiceClient.getUserById(application.getUserId());

                kafkaTemplate.send("create_recent_activity_revenue", RecentActivityRevenueMessage.builder()
                    .userId(application.getUserId())
                    .userName(applicationUser.getName())
                    .jobId(job.getId())
                    .revenue(application.getOfferSalary())
                    .build());
            });

            return new ResponseMessage(200, "Đánh dấu công việc hoàn thành thành công");

        } catch (Exception e) {
            log.error("Lỗi đánh dấu công việc hoàn thành ID {}: {}", jobId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseMessage markJobAsUndone(String jobId, String username) {
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            String userId = user.getId();

            // Kiểm tra quyền: chỉ chủ job mới có thể hủy đánh dấu hoàn thành
            if (!userId.equals(job.getUserId())) {
                throw new RuntimeException("Bạn không phải là người tạo công việc này");
            }

            job.setDone(false);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Job {} đã được hủy đánh dấu hoàn thành bởi user {}", jobId, username);
            return new ResponseMessage(200, "Hủy đánh dấu công việc hoàn thành thành công");

        } catch (Exception e) {
            log.error("Lỗi hủy đánh dấu công việc hoàn thành ID {}: {}", jobId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ResponseMessage testEmJob() {
        List<Job> jobs = jobRepository.findAll();

        for (Job job : jobs) {
            JobEvent jobEvent = JobEvent.builder()
                    .eventType("TEST_EMBEDDING")
                    .data(job)
                    .build();
            kafkaTemplate.send("job_created", jobEvent);
        }
        return ResponseMessage.builder()
                .status(200)
                .message("Cập nhật embedding cho tất cả công việc thành công")
                .build();
    }

    @Override
    public void deleteJobsByCategoryId(String categoryId) {
        try {
            List<Job> jobs = jobRepository.findByCategoryIdAndActiveTrue(categoryId);
            if (jobs.isEmpty()) {
                log.info("Không có công việc nào thuộc danh mục {}", categoryId);
                return;
            }
            for (Job job : jobs) {
                job.setActive(false);
                job.setStatus(false);
                job.setDeletedAt(LocalDateTime.now());
                jobRepository.save(job);
            }
            log.info("Đã xóa tất cả công việc thuộc danh mục {}", categoryId);
        } catch (Exception e) {
            log.error("Lỗi khi xóa công việc theo danh mục {}: {}", categoryId, e.getMessage(), e);
            throw e;
        }
    }


@Override
@Transactional
public ResponseMessage updateJobStatus(String jobId, Boolean status, String username) {
    Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

    job.setStatus(status);
    job.setUpdatedAt(LocalDateTime.now());
    jobRepository.save(job);
    return new ResponseMessage(200, "Cập nhật trạng thái status thành công");
}

    @Override
    @Transactional
    public ResponseMessage updateJobActive(String jobId, Boolean active, String username) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
        UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
        if (!user.getId().equals(job.getUserId())) {
            throw new RuntimeException("Bạn không phải là người tạo công việc này");
        }
        job.setActive(active);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        return new ResponseMessage(200, "Cập nhật trạng thái active thành công");
    }
}