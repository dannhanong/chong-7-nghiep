package com.dan.job_service.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.job_service.dtos.enums.ApplicationStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobApplication {
    @Id
    String id;
    
    @Indexed
    String userId;
    
    @Indexed
    String jobId;
    
    @Indexed
    ApplicationStatus status;
    
    LocalDateTime appliedAt;
    LocalDateTime updatedAt;
    
    // Các trường bổ sung nếu cần
    String coverLetter;
    String cvUrl;
}
