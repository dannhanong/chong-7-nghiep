package com.dan.job_profile_service.services.impl;

import com.dan.events.dtos.JobProfileEvent;
import com.dan.job_profile_service.dtos.requests.ExperienceRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.http_clients.IdentityServiceClient;
import com.dan.job_profile_service.models.Experience;
import com.dan.job_profile_service.repositories.ExperienceRepository;
import com.dan.job_profile_service.services.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {
    private final ExperienceRepository experienceRepository;
    private final IdentityServiceClient identityServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public List<Experience> getAllExperiences(String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        return experienceRepository.findByUserId(userId);
    }

    @Override
    public Experience getExperienceById(String id) {
        return experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kinh nghiệm với id: " + id));
    }

    @Override
    public Experience create(ExperienceRequest experienceRequest, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();

        Experience newExperience = Experience.builder()
                    .userId(userId)
                    .companyName(experienceRequest.getCompanyName())
                    .position(experienceRequest.getPosition())
                    .employmentType(experienceRequest.getEmploymentType())
                    .location(experienceRequest.getLocation())
                    .startDate(experienceRequest.getStartDate())
                    .endDate(experienceRequest.getEndDate())
                    .description(experienceRequest.getDescription())
                    .achievements(experienceRequest.getAchievements())
                    .build();
        Experience savedExperience = experienceRepository.save(newExperience);

        JobProfileEvent jobProfileEvent = JobProfileEvent.builder()
                .userId(userId)
                .build();
        kafkaTemplate.send("profile_created", jobProfileEvent);
        return savedExperience;
    }

    @Override
    public Experience update(ExperienceRequest experienceRequest, String id) {
            Experience existingExperience = getExperienceById(id);

            existingExperience.setCompanyName(experienceRequest.getCompanyName());
            existingExperience.setPosition(experienceRequest.getPosition());
            existingExperience.setEmploymentType(experienceRequest.getEmploymentType());
            existingExperience.setLocation(experienceRequest.getLocation());
            existingExperience.setStartDate(experienceRequest.getStartDate());
            existingExperience.setEndDate(experienceRequest.getEndDate());
            existingExperience.setDescription(experienceRequest.getDescription());
            existingExperience.setAchievements(experienceRequest.getAchievements());

            Experience updatedExperience = experienceRepository.save(existingExperience);
            JobProfileEvent jobProfileEvent = JobProfileEvent.builder()
                    .userId(updatedExperience.getUserId())
                    .build();
            kafkaTemplate.send("profile_updated", jobProfileEvent);
            return updatedExperience;
    }

    @Override
    public ResponseMessage delete(String id) {
        try {
            experienceRepository.deleteById(id);
            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa kinh nghiệm thành công")
                    .build();
        } catch (Exception e) {
            return ResponseMessage.builder()
                    .status(500)
                    .message("Xóa kinh nghiệm thất bại" + e.getMessage())
                    .build();
        }
    }

    @Override
    public List<Experience> getExperienceByUserId(String userId) {
        identityServiceClient.getUserById(userId);
        return experienceRepository.findByUserId(userId);
    }
}
