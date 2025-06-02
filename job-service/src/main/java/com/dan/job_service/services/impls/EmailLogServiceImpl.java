package com.dan.job_service.services.impls;

import com.dan.events.dtos.EventRecommendJob;
import com.dan.job_service.dtos.responses.JobDetailEmail;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.EmailLog;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.Category;
import com.dan.job_service.repositories.EmailLogRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.repositories.CategoryRepository;
import com.dan.job_service.services.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailLogServiceImpl implements EmailLogService {
    private final EmailLogRepository emailLogRepository;
    private final JobRepository jobRepository;
    private final CategoryRepository categoryRepository;
    private final IdentityServiceClient identityServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public EmailLog saveEmailLog(String email, String jobId) {
        EmailLog emailLog = EmailLog.builder()
                .email(email)
                .jobId(jobId)
                .sentAt(LocalDateTime.now())
                .build();
        return emailLogRepository.save(emailLog);
    }

    @Override
    public Map<String, List<String>> filterUnsentJobs(Map<String, List<Map<String, String>>> request) {
        return request.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String email = entry.getKey();
                            List<String> jobIds = entry.getValue().stream()
                                    .map(job -> job.get("_id"))
                                    .toList();

                            List<EmailLog> sentEmails = emailLogRepository.findByEmail(email);

                            Set<String> sentJobIds = sentEmails.stream()
                                    .map(emailLog -> emailLog.getJobId())
                                    .collect(Collectors.toSet());

                            List<String> unsentJobIds = jobIds.stream()
                                    .filter(jobId -> !sentJobIds.contains(jobId))
                                    .collect(Collectors.toList());

                            if (!unsentJobIds.isEmpty()) {
                                List<Job> unsentJobs = unsentJobIds.stream()
                                        .map(jobId -> jobRepository.findById(jobId)
                                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với id: " + jobId)))
                                        .collect(Collectors.toList());

                                List<JobDetailEmail> jobDetailEmails = unsentJobs.stream()
                                        .map(job -> {
                                            Category category = categoryRepository.findById(job.getCategoryId())
                                                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + job.getCategoryId()));
                                            
                                            return JobDetailEmail.builder()
                                                    .title(job.getTitle())
                                                    .categoryName(category.getName())
                                                    .salaryMin(job.getSalaryMin())
                                                    .salaryMax(job.getSalaryMax())
                                                    .applicationDeadline(job.getApplicationDeadline())
                                                    .description(job.getDescription())
                                                    .build();
                                        })
                                        .collect(Collectors.toList());

                                EventRecommendJob eventRecommendJobNotification = EventRecommendJob.builder()
                                        .recipient(email)
                                        .subject("Danh sách công việc mới cho bạn")
                                        .body(jobDetailEmails)
                                        .build();
                                kafkaTemplate.send("job-recommend", email, eventRecommendJobNotification);
                            }
                            return unsentJobIds;
                        }));
    }
}