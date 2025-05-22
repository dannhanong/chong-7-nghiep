package com.dan.identity_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Experience;
import com.dan.identity_service.repositories.ExperienceRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.services.ExperienceService;

@Service
public class ExperienceServiceImpl implements ExperienceService{
    @Autowired
    private ExperienceRepository experienceRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Experience> getExperienceByUsername(String username) {
        String userId = userRepository.findByUsername(username).getId();
        return experienceRepository.findByUserIdAndDeletedAtNull(userId);
    }

    @Override
    public ResponseMessage create(Experience experience, String username) {
        String userId = userRepository.findByUsername(username).getId();
        experience.setUserId(userId);
        experience.setCreatedAt(LocalDateTime.now());
        experience.setUpdatedAt(LocalDateTime.now());
        experience.setDeletedAt(null);
        experienceRepository.save(experience);
        return new ResponseMessage(200, "Thêm thành công");
    }

    @Override
    public ResponseMessage update(String id, String username, Experience experience) {
        String userId = userRepository.findByUsername(username).getId();
        Experience exp = experienceRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);

        if (exp == null) {
            throw new RuntimeException("Không tìm thấy thông tin kinh nghiệm làm việc");
        } else {
            exp.setCompanyName(experience.getCompanyName());
            exp.setStartDate(experience.getStartDate());
            exp.setEndDate(experience.getEndDate());
            exp.setCurrent(experience.isCurrent());
            exp.setDescription(experience.getDescription());
            exp.setUpdatedAt(LocalDateTime.now());
            experienceRepository.save(exp);
            return new ResponseMessage(200, "Cập nhật thành công");
        }
    }

    @Override
    public ResponseMessage delete(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();

        if (!experienceRepository.existsByIdAndUserIdAndDeletedAtNull(id, userId)) {
            throw new RuntimeException("Không tìm thấy thông tin kinh nghiệm làm việc");
        } else {
            Experience experience = experienceRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
            experience.setDeletedAt(LocalDateTime.now());
            experienceRepository.save(experience);
            return new ResponseMessage(200, "Xóa thành công");
        }
    }

    @Override
    public Experience getById(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();
        if (!experienceRepository.existsByIdAndUserIdAndDeletedAtNull(id, userId)) {
            throw new RuntimeException("Không tìm thấy thông tin kinh nghiệm làm việc");
        } else {
            return experienceRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
        }
    }
    
}
