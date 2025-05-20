package com.dan.job_profile_service.services.impl;

import com.dan.job_profile_service.dtos.requests.SkillRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.http_clients.FileServiceClient;
import com.dan.job_profile_service.models.Profile;
import com.dan.job_profile_service.models.Skill;
import com.dan.job_profile_service.repositories.ProfileRepository;
import com.dan.job_profile_service.repositories.SkillRepository;
import com.dan.job_profile_service.services.SkillService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;
    private final FileServiceClient fileServiceClient;

    @Override
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Override
    public Skill getSkillById(String id) {
        Optional<Skill> skill = skillRepository.findById(id);
        if (skill.isPresent()){
            return skill.get();
        }
        throw new ResourceNotFoundException("Không tim thấy kỹ năng với id: "+id);
    }

    @Override
    public Skill create(SkillRequest skillRequest) {
        profileRepository.findById(skillRequest.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ với id: "+skillRequest.getProfileId()));

        Skill newSkill = Skill.builder()
                .profileId(skillRequest.getProfileId())
                .skillName(skillRequest.getSkillName())
                .proficiency(skillRequest.getProficiency())
                .yearsExperience(skillRequest.getYearsExperience())
                .description(skillRequest.getDescription())
                .certifications(skillRequest.getCertifications())
                .build();

        MultipartFile file = skillRequest.getFile();
        if(file != null && !file.isEmpty()){
            Map<String, String> res = fileServiceClient.uploadFile(file);
            String fileCode = res.get("fileCode");
            newSkill.setFile(fileCode);
        }
        return skillRepository.save(newSkill);
    }
    
    @Override
    public Skill update(SkillRequest skillRequest, String id) {
            Skill existingSkill = getSkillById(id);
            existingSkill.setProfileId(skillRequest.getProfileId());
            existingSkill.setSkillName(skillRequest.getSkillName());
            existingSkill.setProficiency(skillRequest.getProficiency());
            existingSkill.setYearsExperience(skillRequest.getYearsExperience());
            existingSkill.setDescription(skillRequest.getDescription());
            existingSkill.setCertifications(skillRequest.getCertifications());

            MultipartFile file = skillRequest.getFile();
            if(file != null && !file.isEmpty()){
                Map<String, String> res = fileServiceClient.uploadFile(file);
                String fileCode = res.get("fileCode");
                existingSkill.setFile(fileCode);
            }
            return skillRepository.save(existingSkill);
    }

    @Override
    public ResponseMessage delete(String id) {
        try {
            skillRepository.deleteById(id);
            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa kỹ năng thành công")
                    .build();
        } catch (Exception e) {
            return ResponseMessage.builder()
                    .status(500)
                    .message("Xóa học vấn thất bại " + e.getMessage())
                    .build();
        }
    }
}
