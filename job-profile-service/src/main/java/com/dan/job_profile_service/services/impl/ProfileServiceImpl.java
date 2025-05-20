package com.dan.job_profile_service.services.impl;

import com.dan.job_profile_service.dtos.requests.ProfileRequest;
import com.dan.job_profile_service.dtos.responses.ProfileFullResponse;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.http_clients.FileServiceClient;
import com.dan.job_profile_service.models.Education;
import com.dan.job_profile_service.models.Experience;
import com.dan.job_profile_service.models.Profile;
import com.dan.job_profile_service.models.Skill;
import com.dan.job_profile_service.repositories.EducationRepository;
import com.dan.job_profile_service.repositories.ExperienceRepository;
import com.dan.job_profile_service.repositories.ProfileRepository;
import com.dan.job_profile_service.repositories.SkillRepository;
import com.dan.job_profile_service.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final FileServiceClient fileServiceClient;

    @Override
    public ProfileFullResponse getFullProfileById(String profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ với id: " + profileId));

        List<Education> educations = educationRepository.findByProfileId(profileId);
        List<Skill> skills = skillRepository.findByProfileId(profileId);
        List<Experience> experiences = experienceRepository.findByProfileId(profileId);

        return ProfileFullResponse.builder()
                .profile(profile)
                .educations(educations)
                .skills(skills)
                .experiences(experiences)
                .build();
    }

    @Override
    public List<Profile> getAllProfiles() {
       return profileRepository.findAll();
    }

    @Override
    public Profile getProfileById(String id) {
        Optional<Profile> profile = profileRepository.findById(id);
        if (profile.isPresent()){
            return profile.get();
        }
        throw new ResourceNotFoundException("Không tim thấy hồ sơ với id: "+id);
    }

    @Override
    public Profile create(ProfileRequest profileRequest) {

        Profile newProfile = Profile.builder()
                .fullName(profileRequest.getFullName())
                .email(profileRequest.getEmail())
                .phoneNumber(profileRequest.getPhoneNumber()).build();

        MultipartFile file = profileRequest.getFile();
        if(file != null && !file.isEmpty()){
            Map<String, String> res = fileServiceClient.uploadFile(file);
            String fileCode = res.get("fileCode");
            newProfile.setFile(fileCode);
        }
        return profileRepository.save(newProfile);
    }

    @Override
    public Profile update(ProfileRequest profileRequest, String id) {
            Profile existingProfile = profileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("hồ sơ không tồn tại"));

            existingProfile.setFullName(profileRequest.getFullName());
            existingProfile.setEmail(profileRequest.getEmail());
            existingProfile.setPhoneNumber(profileRequest.getPhoneNumber());

        return profileRepository.save(existingProfile);
    }

    @Override
    public ResponseMessage delete(String id) {
        try {
            profileRepository.deleteById(id);
            return ResponseMessage.builder()
                    .status(200)
                    .message("Xóa hồ sơ thành công")
                    .build();
        } catch (Exception e) {
            return ResponseMessage.builder()
                    .status(500)
                    .message("Xóa học vấn thất bại " + e.getMessage())
                    .build();
        }
    }
}
