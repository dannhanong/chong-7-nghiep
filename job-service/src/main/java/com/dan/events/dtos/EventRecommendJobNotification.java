package com.dan.events.dtos;


import com.dan.job_service.dtos.responses.JobsLast24HoursResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class EventRecommendJobNotification {
    String recipient;
    String subject;
    List<JobsLast24HoursResponse> jobs;
}