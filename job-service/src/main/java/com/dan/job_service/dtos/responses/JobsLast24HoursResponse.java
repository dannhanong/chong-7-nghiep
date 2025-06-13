package com.dan.job_service.dtos.responses;

import com.dan.job_service.models.Job;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobsLast24HoursResponse {
    String id;
    String title;

    public static JobsLast24HoursResponse fromJobToJobLast24Hours(Job job) {
        return JobsLast24HoursResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .build();
    }
}
