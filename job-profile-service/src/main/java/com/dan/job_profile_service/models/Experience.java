package com.dan.job_profile_service.models;

import com.dan.job_profile_service.dtos.enums.EmploymentType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Document(collection = "experiences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experience extends BaseEntity{
    @Id
    private String id;
    @Indexed
    private String userId;
    //ten cong ty
    @Field("company_name")
    private String companyName;
    //chuc danh
    private String position;
    @Field("employment_type")
    private EmploymentType employmentType; //FULL_TIME, PART_TIME, INTERN, COLLABORATOR
    private String location;
    //bat dau
    @Field("start_date")
    private LocalDate startDate;
    //ket thuc
    @Field("end_date")
    private LocalDate endDate;
    private String description;
    //thanh tuu
    private String achievements;
}
