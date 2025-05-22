package com.dan.identity_service.models;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Skill {
    @Id
    String id;

    @Indexed
    String userId;

    @NotBlank(message = "Tên kỹ năng không được để trống")
    String name;

    @Min(value = 1, message = "Cấp độ kỹ năng từ 1-5")
    @Max(value = 5, message = "Cấp độ kỹ năng từ 1-5")
    Double level;

    List<String> certifications;
    int year;
    String category;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
