package com.dan.job_service.services.impls;

import com.dan.events.dtos.EventRecommendJob;
import com.dan.events.dtos.responses.RecommendJobGmailResponse;
import com.dan.job_service.dtos.responses.JobDetail;
import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.http_clients.RecommendClient;
import com.dan.job_service.models.EmailLog;
import com.dan.job_service.repositories.EmailLogRepository;
import com.dan.job_service.services.EmailLogService;
import com.dan.job_service.services.JobService;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailLogServiceImpl implements EmailLogService {
    private final EmailLogRepository emailLogRepository;
    private final IdentityServiceClient identityServiceClient;
    private final RecommendClient recommendClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JobService jobService;

    @Override
    public EmailLog saveEmailLog(String username, String jobId) {
        EmailLog emailLog = EmailLog.builder()
                .username(username)
                .jobId(jobId)
                .sentAt(LocalDateTime.now())
                .build();
        return emailLogRepository.save(emailLog);
    }

//     @Override
//     public Map<String, List<String>> filterUnsentJobs(Map<String, List<Map<String, String>>> request) {
//         return request.entrySet().stream()
//                 .collect(Collectors.toMap(
//                         Map.Entry::getKey,
//                         entry -> {
//                             String email = entry.getKey();
//                             List<String> jobIds = entry.getValue().stream()
//                                     .map(job -> job.get("_id"))
//                                     .toList();

//                             List<EmailLog> sentEmails = emailLogRepository.findByEmail(email);

//                             Set<String> sentJobIds = sentEmails.stream()
//                                     .map(emailLog -> emailLog.getJobId())
//                                     .collect(Collectors.toSet());

//                             List<String> unsentJobIds = jobIds.stream()
//                                     .filter(jobId -> !sentJobIds.contains(jobId))
//                                     .collect(Collectors.toList());

//                             if (!unsentJobIds.isEmpty()) {
//                                 List<Job> unsentJobs = unsentJobIds.stream()
//                                         .map(jobId -> jobRepository.findById(jobId)
//                                                 .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với id: " + jobId)))
//                                         .collect(Collectors.toList());

//                                 List<JobDetailEmail> jobDetailEmails = unsentJobs.stream()
//                                         .map(job -> {
//                                             Category category = categoryRepository.findById(job.getCategoryId())
//                                                     .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + job.getCategoryId()));
                                            
//                                             return JobDetailEmail.builder()
//                                                     .title(job.getTitle())
//                                                     .categoryName(category.getName())
//                                                     .salaryMin(job.getSalaryMin())
//                                                     .salaryMax(job.getSalaryMax())
//                                                     .applicationDeadline(job.getApplicationDeadline())
//                                                     .description(job.getDescription())
//                                                     .build();
//                                         })
//                                         .collect(Collectors.toList());

//                                 EventRecommendJob eventRecommendJobNotification = EventRecommendJob.builder()
//                                         .recipient(email)
//                                         .subject("Danh sách công việc mới cho bạn")
//                                         .body(jobDetailEmails)
//                                         .build();
//                                 kafkaTemplate.send("job-recommend", email, eventRecommendJobNotification);
//                             }
//                             return unsentJobIds;
//                         }));
//     }

    @Override
    public List<RecommendJobGmailResponse> getGmailJobs() {
        List<RecommendJobGmailResponse> responses = recommendClient.getGmailJobs();

        for (RecommendJobGmailResponse response : responses) {
            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(response.getUsername());
                List<JobDetail> jobDetails;
                if (response.getJob_ids() != null && !response.getJob_ids().isEmpty()) {
                        jobDetails = response.getJob_ids().stream()
                                .map(jobId -> {
                                        if (emailLogRepository.existsByUsernameAndJobId(user.getUsername(), jobId)) {
                                                return null;
                                        }
                                        // Save email log for the job
                                        saveEmailLog(user.getUsername(), jobId);
                                        return jobService.getJobById(jobId, null);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                } else {
                        jobDetails = Collections.emptyList();
                }
            
                EventRecommendJob notificationEvent = EventRecommendJob.builder()
                        .recipient(user.getEmail())
                        .nameOfRecipient(user.getName())
                        .subject("Công việc mới phù hợp với bạn")
                        .body(jobDetails)
                        .build();
                kafkaTemplate.send("job-recommend-gmail", notificationEvent);
        }

        return responses;
    }
}