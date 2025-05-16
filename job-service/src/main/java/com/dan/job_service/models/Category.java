package com.dan.job_service.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "categories")
@CompoundIndexes({
        @CompoundIndex(name = "name_index", unique = true, def = "{'name': 1}"),
        @CompoundIndex(name = "parent_id_index", def = "{'parentId': 1}"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Category {
    @Id
    String id;

    @NotBlank(message = "Tên danh mục không được để trống")
    String name;

    @NotBlank(message = "Mô tả danh mục không được để trống")
    String description;
    
    String parentId;
    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
