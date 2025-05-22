package com.dan.identity_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Education;
import com.dan.identity_service.repositories.EducationRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.services.EducationService;

@Service
public class EducationServiceImpl implements EducationService{
    @Autowired
    private EducationRepository educationRepository;
    @Autowired 
    private UserRepository userRepository;

    @Override
    public List<Education> getEducationByUsername(String username) {
        String userId = userRepository.findByUsername(username).getId();
        return educationRepository.findByUserIdAndDeletedAtNull(userId);
    }

    @Override
    public ResponseMessage create(Education education, String username) {
        String userId = userRepository.findByUsername(username).getId();
        education.setUserId(userId);
        education.setCreatedAt(LocalDateTime.now());
        education.setUpdatedAt(LocalDateTime.now());
        education.setDeletedAt(null);
        educationRepository.save(education);
        return new ResponseMessage(200, "Thêm thành công");
    }

    @Override
    public ResponseMessage update(String id, String username, Education education) {
        String userId = userRepository.findByUsername(username).getId();
        Education e = educationRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
        if (e == null) {
            throw new RuntimeException("Không tìm thấy thông tin học vấn");
        } else {
            e.setSchool(education.getSchool());
            e.setDegree(education.getDegree());
            e.setFieldOfStudy(education.getFieldOfStudy());
            e.setGrade(education.getGrade());
            e.setStartDate(education.getStartDate());
            e.setEndDate(education.getEndDate());
            e.setUpdatedAt(LocalDateTime.now());
            educationRepository.save(e);
            return new ResponseMessage(200, "Cập nhật thành công");
        }
    }

    @Override
    public ResponseMessage delete(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();

        if (!educationRepository.existsByIdAndUserIdAndDeletedAtNull(id, userId)) {
            throw new RuntimeException("Không tìm thấy thông tin học vấn");
        } else {
            Education education = educationRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
            education.setDeletedAt(LocalDateTime.now());
            educationRepository.save(education);
            return new ResponseMessage(200, "Xóa thành công");
        }
    }

    @Override
    public Education getById(String id, String username) {
        String userId = userRepository.findByUsername(username).getId();

        if (!educationRepository.existsByIdAndUserIdAndDeletedAtNull(id, userId)) {
            throw new RuntimeException("Không tìm thấy thông tin học vấn");
        } else {
            return educationRepository.findByIdAndUserIdAndDeletedAtNull(id, userId);
        }
    }
    
}
