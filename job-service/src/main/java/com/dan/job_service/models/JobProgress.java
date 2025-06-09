package com.dan.job_service.models;

import com.dan.job_service.dtos.enums.JobStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "job_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobProgress {
    @Id
    String id;
    @Indexed
    String userId;
    @Indexed
    String jobId;
    JobStatus status;
    @CreatedDate
    LocalDateTime createdAt;
}
