package com.dan.identity_service.services;

import java.util.List;

import com.dan.identity_service.dtos.requests.IdCardRequest;
import com.dan.identity_service.models.IdentityInfo;

public interface IdentityInfoService {
    List<IdentityInfo> getIdentityInfosByUserId(String userId);
    IdentityInfo createIdentityInfo(IdCardRequest idCardRequest, String username);
}
