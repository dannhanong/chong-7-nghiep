package com.dan.job_profile_service.dtos.responses;

import com.dan.job_profile_service.models.Education;
import com.dan.job_profile_service.models.Experience;
import com.dan.job_profile_service.models.Profile;
import com.dan.job_profile_service.models.Skill;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileFullResponse {
    private Profile profile;

    private List<Education> educations;
    private List<Skill> skills;
    private List<Experience> experiences;
}

