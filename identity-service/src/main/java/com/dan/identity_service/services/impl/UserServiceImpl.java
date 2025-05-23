package com.dan.identity_service.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dan.events.dtos.EventFileUpload;
import com.dan.events.dtos.NotificationEvent;
import com.dan.identity_service.dtos.enums.Gender;
import com.dan.identity_service.dtos.enums.ProviderType;
import com.dan.identity_service.dtos.enums.RoleName;
import com.dan.identity_service.dtos.requests.ExchangeTokenRequest;
import com.dan.identity_service.dtos.requests.UpdateProfileRequest;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.dtos.responses.UserProfile;
import com.dan.identity_service.http_clients.FileServiceClient;
import com.dan.identity_service.http_clients.Oauth2IdentityClient;
import com.dan.identity_service.http_clients.Oauth2UserClient;
import com.dan.identity_service.models.Role;
import com.dan.identity_service.models.User;
import com.dan.identity_service.repositories.RoleRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.services.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Oauth2IdentityClient oauth2IdentityClient;
    @Autowired
    private Oauth2UserClient oauth2UserClient;
    @Value("${oauth2.identity.client-id}")
    protected String CLIENT_ID;
    @Value("${oauth2.identity.client-secret}")
    protected String CLIENT_SECRET;
    @Value("${oauth2.identity.redirect-uri}")
    protected String REDIRECT_URI;
    protected final String GRANT_TYPE = "authorization_code";
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private FileServiceClient fileServiceClient;

    // @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    // private String facebookClientId;
    // @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    // private String facebookClientSecret;
    // @Value("${facebook.redirect-uri}")
    // private String facebookRedirectUri;
    // @Autowired
    // private FacebookOauthClient facebookOauthClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng");
        }
        org.springframework.security.core.userdetails.User us = new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), rolesToAuthorities(user.getRoles()));
        return us;
    }

    @Override
    public boolean isEnableUser(String username) {
        User user = userRepository.findByUsername(username);
        return user.isEnabled();
    }

    private Collection<? extends GrantedAuthority> rolesToAuthorities(Collection<Role> roles) {
        return roles.stream().map(role ->new SimpleGrantedAuthority(role.getName().name())).collect(Collectors.toList());
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public UserProfile getProfile(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng");
        }
        return UserProfile.builder()
                .name(user.getName())
                .username(user.getUsername())
                .enabled(user.isEnabled())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRoles().stream()
                    .min(Comparator.comparingInt(this::getRolePriority))
                    .get().getName().toString()
                )
                .avatarCode(user.getAvatarCode())
                .createdAt(user.getCreatedAt())
                .title(user.getTitle())
                .bio(user.getBio())
                .dob(user.getDob())
                .gender(user.getGender())
                .subscribedToNotifications(user.isSubscribedToNotifications())
                .build();
    }

    private int getRolePriority(Role role) {
        switch (role.getName().toString()) {
            case "ADMIN":
                return 1;
            case "RECRUITER":
                return 2;
            case "STAFF":
                return 3;
            case "USER":
                return 4;
            default:
                return Integer.MAX_VALUE;
        }
    }

    @Override
    public User oauth2Authenticate(String code) {
        var response = oauth2IdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());

        var userInfo = oauth2UserClient.getUserInfo("json", response.getAccessToken());
        // log.info("User info: {}", userInfo);

        User user = userRepository.findByEmail(userInfo.getEmail());
        if (user == null) {
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(RoleName.USER);
            roles.add(userRole);

            LocalDateTime now = LocalDateTime.now();

            user = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getGivenName() + " " + userInfo.getFamilyName())
                    .username(userInfo.getEmail())
                    .roles(roles)
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .password("$2a$10$OxGFXT1QNtOHHXXE4G3OVuJ98Mxk6lzMEnFtiH7hQd/LPBmZqQuP.")
                    .providerType(ProviderType.GOOGLE)
                    .subscribedToNotifications(false)
                    .build();
            
            User savedUser = userRepository.save(user);

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .channel("EMAIL")
                    .recipient(savedUser.getEmail())
                    .nameOfRecipient(savedUser.getName())
                    .subject("Chào mừng bạn đến với trang findjob.vn")
                    .body(savedUser.getVerificationCode())
                    .build();
            kafkaTemplate.send("hdkt-notification-oauth2", notificationEvent);
        }

        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng");
        }
        return user;
    }

    @Override
    public void minusJobCount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (user.getJobCount() > 0) {
            user.setJobCount(user.getJobCount() - 1);
            userRepository.save(user);
        }
    }

    @Override
    public void plusJobCount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setJobCount(user.getJobCount() + 1);
        userRepository.save(user);
    }

    @Override
    public User getById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Override
    public ResponseMessage updateProfile(UpdateProfileRequest updateProfileRequest, String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        String email = updateProfileRequest.email();
        String phoneNumber = updateProfileRequest.phoneNumber();
        MultipartFile avatar = updateProfileRequest.avatar();
        if (userRepository.existsByEmailAndUsernameNot(email, username)) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (userRepository.existsByPhoneNumberAndUsernameNot(phoneNumber, username)) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        user.setName(updateProfileRequest.name());
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(updateProfileRequest.address());
        user.setTitle(updateProfileRequest.title());
        user.setBio(updateProfileRequest.bio());
        user.setDob(updateProfileRequest.dob());
        user.setGender(Gender.valueOf(updateProfileRequest.gender().toUpperCase()));
        user.setSubscribedToNotifications(updateProfileRequest.subscribedToNotifications());
        
        if (avatar != null) {
            String oldAvaterCode = user.getAvatarCode();

            String newAvatarCode = fileServiceClient.uploadFile(avatar).get("fileCode").toString();

            user.setAvatarCode(newAvatarCode);
            
            if (oldAvaterCode != null) {
                kafkaTemplate.send("job-delete-file", EventFileUpload.builder()
                    .fileCode(oldAvaterCode)
                    .build()
                );
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return ResponseMessage.builder()
            .status(200)
            .message("Cập nhật thông tin thành công")
            .build();
    }

    @Override
    public void updateCompanyIdForOwner(String ownerId, String companyId) {
        User user = userRepository.findById(ownerId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setCompanyId(companyId);
        userRepository.save(user);
    }
}
