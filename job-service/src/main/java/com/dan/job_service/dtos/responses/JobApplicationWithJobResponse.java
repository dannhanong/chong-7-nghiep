package com.dan.job_service.dtos.responses;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import com.dan.job_service.dtos.enums.WorkingForm;
import com.dan.job_service.dtos.enums.WorkingType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class JobApplicationWithJobResponse {
    // Từ JobApplication
    private String applicationId;
    private String userId;
    private String jobId;
    private ApplicationStatus status; // Trạng thái của đơn ứng tuyển
    private long offerSalary;
    private String offerPlan;
    private String offerSkill;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    // Từ Job
    private String jobTitle;
    private String categoryId;
    private String shortDescription;
    private String description;
    private long salaryMin;
    private long salaryMax;
    private String experienceLevel;
    private String benefits;
    private String requirements;
    private String skills;
    private LocalDate applicationDeadline;
    private Boolean jobStatus; // Đổi tên từ status thành jobStatus
    private Boolean active;
    private WorkingType workingType;
    private WorkingForm workingForm;
    private LocalDateTime jobCreatedAt;
    private LocalDateTime jobUpdatedAt;
}