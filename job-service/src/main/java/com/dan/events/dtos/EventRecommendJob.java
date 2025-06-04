package com.dan.events.dtos;

import com.dan.job_service.dtos.responses.JobDetail;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class EventRecommendJob {
    String recipient;
    String nameOfRecipient;
    String subject;
    List<JobDetail> body;
}