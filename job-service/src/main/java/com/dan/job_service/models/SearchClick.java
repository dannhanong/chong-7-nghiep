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

@Document(collection = "search_clicks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchClick {
    @Id
    String id;
    
    @Indexed
    String userId;
    
    @Indexed
    String jobId;
    
    String searchQuery;
    Integer positionInResults;
    
    @Indexed
    LocalDateTime timestamp;
}
