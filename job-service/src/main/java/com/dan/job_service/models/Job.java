package com.dan.job_service.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.job_service.dtos.enums.WorkingForm;
import com.dan.job_service.dtos.enums.WorkingType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

@Document(collection = "jobs")
@CompoundIndexes({
        @CompoundIndex(name = "company_id_index", def = "{'companyId': 1}"),
        @CompoundIndex(name = "title_index", def = "{'title': 1}"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Job {
    @Id
    String id;
    String userId;

    @NotBlank(message = "Danh mục không được để trống")
    String categoryId;

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;
    String description;

    @Min(value = 0, message = "Lương tối thiểu phải lớn hơn hoặc bằng 0")
    long salaryMin;

    @Min(value = 0, message = "Lương tối đa phải lớn hơn hoặc bằng 0")
    long salaryMax;
    String experienceLevel;
    String benefits;
    String requirements;
    String skills;

    @NotBlank(message = "Hạn nộp hồ sơ không được để trống")
    LocalDate applicationDeadline;
    Boolean status;
    Boolean active;

    WorkingType workingType;
    WorkingForm workingForm;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;

    LocalDateTime deletedAt;
    String contentUri;
}
