package com.dan.job_service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.models.Job;

public interface JobBookmarkService {
    ResponseMessage createBookmark(String jobId, String username);
    ResponseMessage deleteBookmark(String jobId, String username);
    Page<Job> getBookmarks(String username, Pageable pageable);
}
