package com.dan.job_service.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "job_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobView {
    @Id
    String id;
    
    @Indexed
    String userId;
    
    @Indexed
    String jobId;
    
    // Thời gian xem tính bằng giây
    Integer viewDuration;
    
    LocalDateTime viewedAt;
    String userAgent;
}