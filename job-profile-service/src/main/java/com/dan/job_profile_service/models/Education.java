package com.dan.job_profile_service.models;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "educations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education extends BaseEntity{
    @Id
    private String id;
    @Indexed
    private String userId;
    // ten truong
    @Field("school_name")
    @NotBlank(message = "Tên trường không được để trống")
    private String schoolName;
    // bang cap
    private String degree;
    //chuyen nganh
    @NotBlank(message = "Chuyên ngành học không được để trống")
    private String major;
    //bat dau
    @Field("start_date")
    private LocalDateTime startDate;
    //ket thuc
    @Field("end_date")
    private LocalDateTime endDate;
    //diem
    private Double grade;
    // vi tri
    private String location;
}
