package com.dan.job_profile_service.services;

import com.dan.job_profile_service.dtos.requests.SkillRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Skill;

import java.util.List;

public interface SkillService {
    List<Skill> getAllSkills();
    Skill getSkillById(String id);
    Skill create(SkillRequest skillRequest);
    Skill update(SkillRequest skillRequest, String id);
    ResponseMessage delete(String id);
}
