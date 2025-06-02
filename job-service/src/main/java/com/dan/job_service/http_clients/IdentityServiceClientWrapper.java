package com.dan.job_service.http_clients;

import com.dan.job_service.dtos.responses.UserDetailToCreateJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityServiceClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(IdentityServiceClientWrapper.class);

    @Autowired
    private IdentityServiceClient identityServiceClient;

    public UserDetailToCreateJob getUserById(String userId) {
        log.info("Gọi identity-service để lấy thông tin người dùng với userId: {}", userId);
        try {
            UserDetailToCreateJob user = identityServiceClient.getUserById(userId);
            log.info("Phản hồi từ identity-service cho userId {}: {}", userId, user);
            return user;
        } catch (Exception e) {
            log.error("Lỗi khi gọi identity-service để lấy thông tin người dùng với userId {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public UserDetailToCreateJob getUserByUsername(String username) {
        log.info("Gọi identity-service để lấy thông tin người dùng với username: {}", username);
        try {
            UserDetailToCreateJob user = identityServiceClient.getUserByUsername(username);
            log.info("Phản hồi từ identity-service cho username {}: {}", username, user);
            return user;
        } catch (Exception e) {
            log.error("Lỗi khi gọi identity-service để lấy thông tin người dùng với username {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
}