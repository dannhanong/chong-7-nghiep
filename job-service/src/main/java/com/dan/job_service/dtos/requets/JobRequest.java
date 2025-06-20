package com.dan.job_service.dtos.requets;

import java.time.LocalDate;
import java.util.List;

import com.dan.job_service.dtos.enums.WorkingForm;
import com.dan.job_service.dtos.enums.WorkingType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record JobRequest(
    String categoryId,
    @NotBlank(message = "Tiêu đề không được để trống")
    String title,
    String description,
     String shortDescription,
    @Min(value = 0, message = "Lương tối thiểu phải lớn hơn hoặc bằng 0")
    long salaryMin,
    @Min(value = 0, message = "Lương tối đa phải lớn hơn hoặc bằng 0")
    long salaryMax,
    String experienceLevel,
    String benefits, 
    @NotBlank(message = "Hạn nộp hồ sơ không được để trống")   
    LocalDate applicationDeadline,
    String contentUri,
    WorkingForm workingForm,
    WorkingType workingType,
    MultipartFile file,
    List<MultipartFile> otherFiles,
    List<String> tags,
    Boolean status,
    Boolean active,
    Boolean done
) {}
