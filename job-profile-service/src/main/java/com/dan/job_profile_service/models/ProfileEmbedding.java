package com.dan.job_profile_service.models;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "profile_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileEmbedding {
    @Id
    String id;
    
    @Indexed
    String userId;
    
    List<Float> embedding;
    LocalDateTime updatedAt;
}
