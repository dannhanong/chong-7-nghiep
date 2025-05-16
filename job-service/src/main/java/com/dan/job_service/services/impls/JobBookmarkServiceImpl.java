package com.dan.job_service.services.impls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.Job;
import com.dan.job_service.models.JobBookmark;
import com.dan.job_service.repositories.JobBookmarkRepository;
import com.dan.job_service.repositories.JobRepository;
import com.dan.job_service.services.JobBookmarkService;

@Service
public class JobBookmarkServiceImpl implements JobBookmarkService{
    @Autowired
    private JobBookmarkRepository jobBookmarkRepository;
    @Autowired
    private IdentityServiceClient identityServiceClient;
    @Autowired
    private JobRepository jobRepository;

    @Override
    public ResponseMessage createBookmark(String jobId, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        
        if (jobBookmarkRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new RuntimeException("Bạn đã lưu công việc này rồi");
        }

        jobBookmarkRepository.save(JobBookmark.builder()
            .userId(userId)
            .jobId(jobId)
            .active(true)
            .build()
        );

        return ResponseMessage.builder()
            .status(200)
            .message("Lưu công việc thành công")
            .build();
    }

    @Override
    public ResponseMessage deleteBookmark(String jobId, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        
        JobBookmark jobBookmark = jobBookmarkRepository.findByUserIdAndJobId(userId, jobId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc đã lưu"));

        jobBookmark.setActive(false);
        jobBookmarkRepository.save(jobBookmark);

        return ResponseMessage.builder()
            .status(200)
            .message("Xóa công việc đã lưu thành công")
            .build();
    }

    @Override
    public Page<Job> getBookmarks(String username, Pageable pageable) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        Page<JobBookmark> jobBookmarks = jobBookmarkRepository.findByUserIdAndActiveTrue(userId, pageable);
        
        return jobBookmarks.map(jobBookmark -> jobRepository.findById(jobBookmark.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobBookmark.getJobId())));
    }
}
