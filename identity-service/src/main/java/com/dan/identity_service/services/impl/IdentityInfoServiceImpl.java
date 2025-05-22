package com.dan.identity_service.services.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dan.identity_service.dtos.enums.Gender;
import com.dan.identity_service.dtos.requests.IdCardRequest;
import com.dan.identity_service.models.IdentityInfo;
import com.dan.identity_service.models.User;
import com.dan.identity_service.repositories.IdentityInfoRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.services.IdentityInfoService;

@Service
public class IdentityInfoServiceImpl implements IdentityInfoService{
    @Autowired
    private IdentityInfoRepository identityInfoRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<IdentityInfo> getIdentityInfosByUserId(String userId) {
        return identityInfoRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin danh tính cho người dùng này"));
    }

    @Override
    public IdentityInfo createIdentityInfo(IdCardRequest idCardRequest, String username) {
        User user = userRepository.findByUsername(username);
        IdentityInfo identityInfo = IdentityInfo.builder()
            .name(idCardRequest.getName())
            .dob(parseDate(idCardRequest.getDob()))
            .gender(idCardRequest.getGender().equals("Nam") ? Gender.MALE : Gender.FEMALE)
            .nationality(idCardRequest.getNationality())
            .country(idCardRequest.getCountry())
            .address(idCardRequest.getAddress())
            .idNumber(idCardRequest.getId_number())
            .expiryDate(parseDate(idCardRequest.getExpiry_date()))
            .userId(user.getId())
            .build();

        user.setIdentityVerified(true);
        userRepository.save(user);
        return identityInfoRepository.save(identityInfo);
    }
    
    public LocalDate parseDate(String dateString) {
        String[] parts = dateString.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        return LocalDate.of(year, month, day);
    }
}
