package com.dan.job_profile_service.services;

import com.dan.job_profile_service.dtos.requests.SkillRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SkillService {
    List<Skill> getAllSkills(String username);
    Skill getSkillById(String id);
    Skill create(SkillRequest skillRequest, String username);
    Skill update(SkillRequest skillRequest, String id);
    ResponseMessage delete(String id);
    List<Skill> getSkillByUserId(String userId);
}
