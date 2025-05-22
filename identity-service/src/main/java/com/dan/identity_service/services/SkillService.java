package com.dan.identity_service.services;

import java.util.List;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Skill;

public interface SkillService {
    List<Skill> getSkillByUsername(String username);
    ResponseMessage create(Skill skill, String username);
    ResponseMessage update(String id, String username, Skill skill);
    ResponseMessage delete(String id, String username);
    Skill getById(String id, String username);
}
