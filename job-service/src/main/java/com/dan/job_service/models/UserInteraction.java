package com.dan.job_service.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.job_service.dtos.enums.InteractionType;
import com.dan.job_service.dtos.enums.ItemType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "user_interactions")
@CompoundIndexes({
    @CompoundIndex(name = "user_item_index", def = "{'userId': 1, 'itemId': 1, 'itemType': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInteraction {
    @Id
    String id;
    
    @Indexed
    String userId;
    
    @Indexed
    String itemId; 
    
    ItemType itemType; // "job", "company", "course", etc.
    InteractionType interactionType; // "view", "bookmark", "application", "rating", "click"
    
    Double interactionScore;
    
    @Indexed
    LocalDateTime timestamp;
}
