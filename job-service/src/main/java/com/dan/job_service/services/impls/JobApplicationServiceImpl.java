package com.dan.job_service.services.impls;

import com.dan.events.dtos.RecentActivityApplicationMessage;
import com.dan.job_service.controllers.JobApplicationController;
import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.JobApplicationResponse;
import com.dan.job_service.dtos.responses.JobApplicationDetailResponse;
import com.dan.job_service.dtos.responses.JobApplicationProfileResponse;
import com.dan.job_service.dtos.responses.JobApplicationWithJobResponse;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import com.dan.job_service.dtos.responses.UserProfileDetail;
import com.dan.job_service.dtos.responses.UserProfileDetailResponse;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.http_clients.ProfileServiceClient;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.JobApplication;
import com.dan.job_service.repositories.JobApplicationRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final IdentityServiceClient identityServiceClient;
    private final ProfileServiceClient profileServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public ResponseMessage applyJob(JobApplicationRequest request, String jobId, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        if (jobApplicationRepository.findByUserIdAndJobId(userId, jobId).isPresent()) {
            throw new RuntimeException("Bạn đã ứng tuyển công việc này");
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
    public ResponseMessage updateStatus(String id, String status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển với id: " + id));

        jobApplication.setStatus(ApplicationStatus.valueOf(status));

        if (status.equalsIgnoreCase("APPROVED")) {
            Job job = jobRepository.findById(jobApplication.getJobId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

            kafkaTemplate.send("create_recent_activity_application", RecentActivityApplicationMessage.builder()
                    .job(JobDetail.builder()
                            .id(job.getId())
                            .title(job.getTitle())
                            .userId(job.getUserId())
                            .build())
                    .user(identityServiceClient.getUserById(jobApplication.getUserId()))
                    .build());
        }
        
        jobApplication.setUpdatedAt(LocalDateTime.now());

        jobApplicationRepository.save(jobApplication);

        return ResponseMessage.builder()
                .status(200)
                .message("Cập nhật trạng thái đơn ứng tuyển thành công")
                .build();
    }

    @Override
    public Page<JobApplicationResponse> getJobApplicationByUserId(String username, Pageable pageable) {
        try {
            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            logger.info("User ID: {}", user.getId());
            logger.info("User: {}", user);
            logger.info("Username: {}", username);
            if (user == null || user.getId() == null) {
                throw new RuntimeException("Không tìm thấy thông tin người dùng");
            }

            Page<JobApplication> jobApplications = jobApplicationRepository.findByUserId(user.getId(), pageable);

            List<JobApplicationResponse> responseList = jobApplications.getContent().stream()
                    .map(application -> {
                        Job job = jobRepository.findById(application.getJobId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

                        UserProfileDetailResponse userProfile = profileServiceClient
                                .getProfileByUserId(application.getUserId());
                        long countApplied = countAppliedSuccess(user.getId());

                        return JobApplicationResponse.builder()
                                .id(application.getId())
                                .userId(application.getUserId())
                                .jobId(application.getJobId())
                                .name(userProfile.getName())
                                .title(job.getTitle())
                                .status(application.getStatus())
                                .offerSalary(application.getOfferSalary())
                                .offerPlan(application.getOfferPlan())
                                .offerSkill(application.getOfferSkill())
                                .appliedAt(application.getAppliedAt())
                                .updatedAt(application.getUpdatedAt())
                                .enabled(userProfile.isEnabled())
                                .email(user.getEmail())
                                .linkPage(user.getLinkPage())
                                .dob(userProfile.getDob())
                                .phoneNumber(userProfile.getPhoneNumber())
                                .avatarId(userProfile.getAvatarId())
                                .countApplied(countApplied)
                                .build();
                    })
                    .collect(Collectors.toList());

            return new PageImpl<>(responseList, pageable, jobApplications.getTotalElements());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách đơn ứng tuyển: " + e.getMessage());
        }
    }

    @Override
    public Page<JobApplicationResponse> getJobApplicationByJobId(String jobId, String username, Pageable pageable) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        if (!job.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem đơn ứng tuyển");
        }

        Page<JobApplication> jobApplications = jobApplicationRepository.findByJobId(jobId, pageable);

        List<JobApplicationResponse> responseList = jobApplications.getContent().stream()
                .map(application -> {
                    UserDetailToCreateJob user = identityServiceClient.getUserById(application.getUserId());
                    UserProfileDetailResponse userProfile = profileServiceClient
                            .getProfileByUserId(application.getUserId());
                    long countApplied = countAppliedSuccess(application.getUserId());

                    return JobApplicationResponse.builder()
                            .id(application.getId())
                            .userId(application.getUserId())
                            .jobId(application.getJobId())
                            .name(userProfile.getName())
                            .userName(userProfile.getUserName())
                            .title(job.getTitle())
                            .status(application.getStatus())
                            .offerSalary(application.getOfferSalary())
                            .offerPlan(application.getOfferPlan())
                            .offerSkill(application.getOfferSkill())
                            .appliedAt(application.getAppliedAt())
                            .updatedAt(application.getUpdatedAt())
                            .enabled(userProfile.isEnabled())
                            .email(user.getEmail())
                            .linkPage(user.getLinkPage())
                            .dob(userProfile.getDob())
                            .phoneNumber(userProfile.getPhoneNumber())
                            .avatarId(userProfile.getAvatarId())
                            .countApplied(countApplied)

                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, jobApplications.getTotalElements());
    }

    @Override
    public JobApplicationResponse getJobApplicationByJobId(String jobId, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        List<JobApplication> lstApplication = jobApplicationRepository.findByJobId(jobId);

        JobApplication application = null;
        UserProfileDetailResponse userProfile = null;
        for (JobApplication jobApplication : lstApplication) {
            logger.info("Compare: jobApplication.getUserId() = {}, userId = {}", jobApplication.getUserId(), userId);
            if (userId != null && userId.equals(jobApplication.getUserId())) {
                logger.info("Found userId: {}", jobApplication.getUserId());
                application = jobApplication;
                userProfile = profileServiceClient.getProfileByUserId(application.getUserId());
                break;
            }
        }

        // Thêm điều kiện nếu không tìm thấy
        if (application == null) {
            throw new RuntimeException("Không tìm thấy đơn ứng tuyển của người dùng này cho công việc này");
        }

        return JobApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .jobId(application.getJobId())
                .title(job.getTitle())
                .userName(username)
                .status(application.getStatus())
                .offerSalary(application.getOfferSalary())
                .offerPlan(application.getOfferPlan())
                .offerSkill(application.getOfferSkill())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    @Override
    public Page<JobApplicationProfileResponse> getPublicJobApplicationByJobId(String jobId, Pageable pageable) {
        jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + jobId));

        Page<JobApplication> jobApplications = jobApplicationRepository.findByJobId(jobId, pageable);

        if (jobApplications.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển cho công việc với ID: " + jobId);
        }

        // --- PHẦN THAY ĐỔI BẮT ĐẦU TỪ ĐÂY ---

        // Sử dụng một Set để lưu trữ các userId đã được xử lý
        Set<String> seenUserIds = new HashSet<>();

        List<JobApplicationProfileResponse> responseList = jobApplications.getContent().stream()
                // Lọc danh sách, chỉ giữ lại những application có userId chưa xuất hiện
                .filter(application -> seenUserIds.add(application.getUserId()))

                // Ánh xạ những application đã được lọc
                .map(application -> {
                    UserProfileDetail userProfile = profileServiceClient
                            .getPublicProfileByUserId(application.getUserId());

                    Integer totalCountJobDone = jobApplicationRepository.countByUserIdAndStatus(application.getUserId(),
                            ApplicationStatus.APPROVED);

                    return JobApplicationProfileResponse.builder()
                            .id(application.getId())
                            .userId(userProfile.getUserId())
                            .name(userProfile.getName())
                            .enabled(userProfile.isEnabled())
                            .email(userProfile.getEmail())
                            .dob(userProfile.getDob())
                            .avatarId(userProfile.getAvatarId())
                            .pathName(userProfile.getPathName())
                            .averageRating(userProfile.getAverageRating())
                            .skills(userProfile.getSkills())
                            .appliedAt(application.getAppliedAt())
                            .offerPlan(application.getOfferPlan())
                            .offerSkill(application.getOfferSkill())
                            .offerSalary(application.getOfferSalary())
                            .status(application.getStatus())
                            .totalCountJobDone(totalCountJobDone)
                            .build();
                })
                .collect(Collectors.toList());

        // Lưu ý: Tổng số phần tử trả về có thể khác với tổng số phần tử ban đầu
        // sau khi đã lọc. Chúng ta sẽ tạo Page mới với số lượng thực tế.

        logger.info("Total elements after filtering: {}", responseList);
        return new PageImpl<>(responseList, pageable, jobApplications.getTotalElements());
        // Nếu bạn muốn totalElements phản ánh số lượng sau khi lọc, bạn cần
        // một truy vấn khác để đếm số lượng userId duy nhất.
    }

    @Override
    public long countAppliedSuccess(String userId) {
        return jobApplicationRepository.countApprovedApplicationsByUserId(userId);
    }

    @Override
    public Object getJobApplicationDetail(String applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển"));

        Job job = jobRepository.findById(application.getJobId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        // Get client info
        UserDetailToCreateJob clientUser = identityServiceClient.getUserById(job.getUserId());
        UserDetailToCreateJob freelancerUser = identityServiceClient.getUserById(application.getUserId());

        return JobApplicationDetailResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .jobTitle(job.getTitle())
                .clientUsername(clientUser.getUsername())
                .freelancerUsername(freelancerUser.getUsername())
                .status(application.getStatus().toString())
                .completedAt(application.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .jobDone(job.getDone() != null ? job.getDone() : false) // Thêm job done status
                .jobStatus(job.getStatus() != null ? job.getStatus().toString() : "UNKNOWN") // Thêm job status
                .build();
    }

    @Override
    public void deleteByJobId(String jobId) {
        List<JobApplication> applications = jobApplicationRepository.findByJobId(jobId);
        if (applications.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển cho công việc với ID: " + jobId);
        }

        for (JobApplication application : applications) {
            application.setDeleteAt(LocalDateTime.now());
            jobApplicationRepository.save(application);
        }
    }

    @Override
    public ResponseMessage delete(String id) {
        return jobApplicationRepository.findById(id).map(jobApplication -> {
            jobApplication.setDeleteAt(LocalDateTime.now());
            jobApplicationRepository.save(jobApplication);

            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa đơn ứng tuyển thành công")
                    .build();
        }).orElseThrow(() -> new RuntimeException("Đơn ứng tuyển không tồn tại"));
    }

}
