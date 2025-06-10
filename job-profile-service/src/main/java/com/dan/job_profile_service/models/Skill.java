package com.dan.job_profile_service.models;

import com.dan.job_profile_service.dtos.enums.ProficiencyType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "skills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill extends BaseEntity{
    @Id
    private String id;
    @Indexed
    private String userId;
    //ten ky nang
    @Field("skill_name")
    @NotBlank(message = "Tên kỹ năng không được để trống")
    private String skillName;
    //muc do thanh thao
    private ProficiencyType proficiency; //BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    //so nam kinh nghiem
    @Field("years_experience")
    private Double yearsExperience;
    //chung chi
    private String certifications;
    private String description;
    private String file;
}
