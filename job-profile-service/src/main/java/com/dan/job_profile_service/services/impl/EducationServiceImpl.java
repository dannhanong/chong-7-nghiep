package com.dan.job_profile_service.services.impl;

import com.dan.events.dtos.JobProfileEvent;
import com.dan.job_profile_service.dtos.requests.EducationRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.http_clients.IdentityServiceClient;
import com.dan.job_profile_service.models.Education;
import com.dan.job_profile_service.repositories.EducationRepository;
import com.dan.job_profile_service.services.EducationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {
    private final EducationRepository educationRepository;
    private final IdentityServiceClient identityServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public List<Education> getAllEducations() {
        return educationRepository.findAll();
    }

    @Override
    public Education getEducationById(String id) {
        return educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học vấn với id: " + id));
    }

    @Override
    public Education create(EducationRequest educationRequest, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();

        Education education = Education.builder()
                    .userId(userId)
                    .schoolName(educationRequest.getSchoolName())
                    .degree(educationRequest.getDegree())
                    .major(educationRequest.getMajor())
                    .startDate(educationRequest.getStartDate())
                    .endDate(educationRequest.getEndDate())
                    .grade(educationRequest.getGrade())
                    .location(educationRequest.getLocation())
                    .build();

            Education savedEducation = educationRepository.save(education);

            JobProfileEvent jobProfileEvent = JobProfileEvent.builder()
                    .userId(userId)
                    .build();
        kafkaTemplate.send("profile_created", jobProfileEvent);
        return savedEducation;
    }

    @Override
    public Education update(EducationRequest educationRequest, String id) {
            Education existingEducation = getEducationById(id);

            existingEducation.setSchoolName(educationRequest.getSchoolName());
            existingEducation.setDegree(educationRequest.getDegree());
            existingEducation.setMajor(educationRequest.getMajor());
            existingEducation.setStartDate(educationRequest.getStartDate());
            existingEducation.setEndDate(educationRequest.getEndDate());
            existingEducation.setGrade(educationRequest.getGrade());
            existingEducation.setLocation(educationRequest.getLocation());

            Education updatedEducation = educationRepository.save(existingEducation);
            JobProfileEvent jobProfileEvent = JobProfileEvent.builder()
                    .userId(updatedEducation.getUserId())
                    .build();
            kafkaTemplate.send("profile_updated", jobProfileEvent);
            return updatedEducation;
    }

    @Override
    public ResponseMessage delete(String id) {
        try {
            educationRepository.deleteById(id);
            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa học vấn thành công")
                    .build();
        } catch (Exception e) {
            return ResponseMessage.builder()
                    .status(500)
                    .message("Xóa học vấn thất bại")
                    .build();
        }
    }
}
