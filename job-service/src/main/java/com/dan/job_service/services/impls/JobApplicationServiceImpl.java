package com.dan.job_service.services.impls;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.requets.JobApplicationRequest;
import com.dan.job_service.dtos.responses.JobApplicationResponse;
import com.dan.job_service.dtos.responses.JobApplicationDetailResponse;
import com.dan.job_service.dtos.responses.JobApplicationWithJobResponse;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final IdentityServiceClient identityServiceClient;
    private final ProfileServiceClient profileServiceClient;

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
    public ResponseMessage updateStatus(String id, String status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển với id: " + id));

        jobApplication.setStatus(ApplicationStatus.valueOf(status));
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
            if (user == null || user.getId() == null) {
                throw new RuntimeException("Không tìm thấy thông tin người dùng");
            }

            Page<JobApplication> jobApplications = jobApplicationRepository.findByUserId(user.getId(), pageable);

            List<JobApplicationResponse> responseList = jobApplications.getContent().stream()
                .map(application -> {
                    Job job = jobRepository.findById(application.getJobId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
                    
                    UserProfileDetailResponse userProfile = profileServiceClient.getProfileByUserId(application.getUserId());
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
                UserProfileDetailResponse userProfile = profileServiceClient.getProfileByUserId(application.getUserId());
                long countApplied = countAppliedSuccess(application.getUserId());

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
}
