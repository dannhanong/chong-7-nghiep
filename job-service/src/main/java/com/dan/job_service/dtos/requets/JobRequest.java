package com.dan.job_service.dtos.requets;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record JobRequest(
    String categoryId,
    @NotBlank(message = "Tiêu đề không được để trống")
    String title,
    String description,
    @Min(value = 0, message = "Lương tối thiểu phải lớn hơn hoặc bằng 0")
    long salaryMin,
    @Min(value = 0, message = "Lương tối đa phải lớn hơn hoặc bằng 0")
    long salaryMax,
    String experienceLevel,
    String benefits, 
    @NotBlank(message = "Hạn nộp hồ sơ không được để trống")   
    LocalDate applicationDeadline,
    String contentUri
) {}
