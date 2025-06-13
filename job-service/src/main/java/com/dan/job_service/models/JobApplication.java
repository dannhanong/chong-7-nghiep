package com.dan.job_service.models;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;

import org.springframework.cglib.core.Local;
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

    // muc gia de xuat
    @Min(value = 0, message = "Lương tối thiểu phải lớn hơn hoặc bằng 0")
    long offerSalary;
    // mo ta ke hoach thuc hien
    String offerPlan;
    // mo ta kinh nghiem phu hpo voi du an
    String offerSkill;
    
    LocalDateTime appliedAt;
    LocalDateTime updatedAt;
    LocalDateTime deleteAt;
}
