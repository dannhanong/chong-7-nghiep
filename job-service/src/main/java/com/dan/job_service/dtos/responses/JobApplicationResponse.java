package com.dan.job_service.dtos.responses;

import com.dan.job_service.dtos.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {
    private String id;
    private String userId;
    private String jobId;
    private String name;
    private String userName;
    private String title;
    private ApplicationStatus status;
    private long offerSalary;
    private String offerPlan;
    private String offerSkill;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private boolean enabled;
    private String email;
    private String roles;
    private String linkPage;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date dob;
    private String phoneNumber;
    private String avatarId;
    private long countApplied;
} 