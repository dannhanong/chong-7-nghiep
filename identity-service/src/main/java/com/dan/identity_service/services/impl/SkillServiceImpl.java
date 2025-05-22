package com.dan.identity_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Skill;
import com.dan.identity_service.repositories.SkillRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.services.SkillService;

@Service
public class SkillServiceImpl implements SkillService{
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Skill> getSkillByUsername(String username) {
        String userId = userRepository.findByUsername(username).getId();
        return skillRepository.findByUserIdAndDeletedAtNull(userId);
    }

    @Override
    public ResponseMessage create(Skill skill, String username) {
        String userId = userRepository.findByUsername(username).getId();
        skill.setUserId(userId);
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        skill.setDeletedAt(null);
        skillRepository.save(skill);
        return new ResponseMessage(200, "Thêm thành công");
    }

    @Override
    public ResponseMessage update(String id, String username, Skill skill) {
        String userId = userRepository.findByUsername(username).getId();
        Skill s = skillRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);

        if (s == null) {
            throw new RuntimeException("Không tìm thấy thông tin kỹ năng");
        } else {
            s.setName(skill.getName());
            s.setLevel(skill.getLevel());
            s.setCertifications(skill.getCertifications());
            s.setYear(skill.getYear());
            s.setCategory(skill.getCategory());
            s.setUpdatedAt(LocalDateTime.now());
            s.setDeletedAt(null);
            skillRepository.save(s);
            return new ResponseMessage(200, "Cập nhật thành công");
            
        }
    }

    @Override
    public ResponseMessage delete(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();

        if (!skillRepository.existsByIdAndUserIdAndDeletedAtNull(id, userId)) {
            throw new RuntimeException("Không tìm thấy thông tin kỹ năng");
        } else {
            Skill skill = skillRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
            skill.setDeletedAt(LocalDateTime.now());
            skillRepository.save(skill);
            return new ResponseMessage(200, "Xóa thành công");   
        }
    }

    @Override
    public Skill getById(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();
        return skillRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
    }
    
}
