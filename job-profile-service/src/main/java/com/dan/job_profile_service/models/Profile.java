package com.dan.job_profile_service.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends BaseEntity{
    @Id
    private String id;
    //ten ung vien
    @Field("full_name")
    private String fullName;
    private String email;
    private Experience experience;
    private Education education;
    private Skill skill;
}
