package com.dan.job_service.controllers;

import java.time.LocalDateTime;


import com.dan.job_service.repositories.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dan.events.dtos.EventAddJobDataForRecommend;
import com.dan.job_service.models.SearchClick;
import com.dan.job_service.repositories.JobViewRepository;
import com.dan.job_service.repositories.SearchClickRepository;

@Component
public class EventController {
    @Autowired
    private SearchClickRepository searchClickRepository;
    @Autowired
    private JobViewRepository jobViewRepository;
    @Autowired
    private JobRepository jobRepository;

    @KafkaListener(topics = "job_get_job_by_id")
    public void listenGetJobById(EventAddJobDataForRecommend message) {
        searchClickRepository.save(SearchClick.builder()
                .userId(message.getUserId())
                .jobId(message.getJobId())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
