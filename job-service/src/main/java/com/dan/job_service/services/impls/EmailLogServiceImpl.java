package com.dan.job_service.services.impls;

import com.dan.job_service.models.EmailLog;
import com.dan.job_service.repositories.EmailLogRepository;
import com.dan.job_service.services.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailLogServiceImpl implements EmailLogService {

    private final EmailLogRepository emailLogRepository;

    @Override
    public EmailLog saveEmailLog(String username, String jobId) {
        EmailLog emailLog = EmailLog.builder()
                .username(username)
                .jobId(jobId)
                .sentAt(LocalDateTime.now())
                .build();
        return emailLogRepository.save(emailLog);
    }

    @Override
    public List<String> filterUnsentJobs(String username, List<String> jobIds) {
        // Lấy danh sách job đã gửi cho user này
        List<EmailLog> sentEmails = emailLogRepository.findByUsername(username);

        // Lấy danh sách jobId đã gửi
        Set<String> sentJobIds = sentEmails.stream()
                .map(EmailLog::getJobId)
                .collect(Collectors.toSet());

        // Lọc ra các job chưa gửi
        return jobIds.stream()
                .filter(jobId -> !sentJobIds.contains(jobId))
                .collect(Collectors.toList());
    }
}