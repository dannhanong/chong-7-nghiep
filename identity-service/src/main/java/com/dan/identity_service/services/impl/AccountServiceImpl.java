package com.dan.identity_service.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.dan.identity_service.dtos.enums.Gender;
import com.dan.identity_service.dtos.enums.ProviderType;
import com.dan.identity_service.dtos.enums.RoleName;
import com.dan.identity_service.dtos.requests.LoginRequest;
import com.dan.identity_service.dtos.requests.SignupRequest;
import com.dan.identity_service.dtos.requests.StaffAccountRequest;
import com.dan.identity_service.dtos.responses.LoginResponse;
import com.dan.identity_service.dtos.responses.ResponseMessage;
import com.dan.identity_service.models.Role;
import com.dan.identity_service.models.User;
import com.dan.identity_service.repositories.RoleRepository;
import com.dan.identity_service.repositories.UserRepository;
import com.dan.identity_service.security.jwt.JwtService;
import com.dan.identity_service.services.AccountService;
import com.dan.identity_service.services.UserService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @Override
    public User signup(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.username())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(signupRequest.phoneNumber())) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        if (!signupRequest.password().equals(signupRequest.confirmPassword())) {
            throw new RuntimeException("Mật khẩu không khớp");
        }

        Set<Role> roles = new HashSet<>();
        String role = signupRequest.role();
        if (role == null){
            Role userRole = roleRepository.findByName(RoleName.USER);
            roles.add(userRole);
        }else {
            switch (role){
                case "admin":
                    Role adminRole = roleRepository.findByName(RoleName.ADMIN);
                    Role adminRecruiterRole = roleRepository.findByName(RoleName.RECRUITER);
                    Role adminStaffRole = roleRepository.findByName(RoleName.STAFF);
                    Role adminUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(adminRole);
                    roles.add(adminRecruiterRole);
                    roles.add(adminStaffRole);
                    roles.add(adminUserRole);
                    break;
                case "recruiter":
                    Role recruiterRole = roleRepository.findByName(RoleName.RECRUITER);
                    Role recruiterStaffRole = roleRepository.findByName(RoleName.STAFF);
                    Role recruiterUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(recruiterRole);
                    roles.add(recruiterStaffRole);
                    roles.add(recruiterUserRole);
                    break;
                case "staff":
                    Role staffRole = roleRepository.findByName(RoleName.STAFF);
                    Role staffUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(staffRole);
                    roles.add(staffUserRole);
                    break;
                case "user":
                    Role userRole = roleRepository.findByName(RoleName.USER);
                    roles.add(userRole);
                    break;
            }
        }

        User user = User.builder()
            .name(signupRequest.name())
            .username(signupRequest.username())
            .email(signupRequest.email())
            .phoneNumber(signupRequest.phoneNumber())
            .password(passwordEncoder.encode(signupRequest.password()))
            .roles(roles)
            .enabled(false)
            .jobCount(1)
            .verificationCode(UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .identityVerified(false)
            .providerType(ProviderType.LOCAL)
            .build();

        User savedUser = userRepository.save(user);

        // kafkaTemplate.send("job-create-user-blockchain", EventCreateUser.builder()
        //     .id(savedUser.getId())
        //     .username(savedUser.getUsername())
        //     .email(savedUser.getEmail())
        //     .phoneNumber(savedUser.getPhoneNumber())
        //     .address(savedUser.getAddress() == null ? "" : savedUser.getAddress())
        //     .enabled(savedUser.isEnabled())
        //     .role(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList()).get(0).name().toLowerCase())
        //     .build()
        // );
        return userRepository.save(savedUser);
    }

    @Override
    public ResponseMessage verify(String token) {
        User user = userRepository.findByVerificationCode(token);
        if (user == null) {
            throw new RuntimeException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }
        user.setEnabled(true);
        user.setVerificationCode(null);
        User userVerified = userRepository.save(user);

        kafkaTemplate.send("job-verify-user-blockchain", userVerified.getId());

        return ResponseMessage.builder()
            .status(200)
            .message("Xác thực tài khoản thành công")
            .build();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse tokens = new LoginResponse();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

            if (!userService.isEnableUser(loginRequest.username())) {
                throw new RuntimeException("Tài khoản chưa được xác minh hoặc đã bị khóa");
            }

            if (authentication.isAuthenticated()) {
                final String accessToken = jwtService.generateToken(loginRequest.username(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
                final String refreshToken = jwtService.generateRefreshToken(loginRequest.username(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

                tokens = LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .userProfile(userService.getProfile(loginRequest.username()))
                        .build();               
            }
        } catch (AuthenticationException e) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }    
        return tokens;
    }

    @Override
    public User createByAdmin(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.username())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(signupRequest.phoneNumber())) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        if (!signupRequest.password().equals(signupRequest.confirmPassword())) {
            throw new RuntimeException("Mật khẩu không khớp");
        }

        Set<Role> roles = new HashSet<>();
        String role = signupRequest.role();
        if (role == null){
            Role userRole = roleRepository.findByName(RoleName.USER);
            roles.add(userRole);
        }else {
            switch (role){
                case "admin":
                    Role adminRole = roleRepository.findByName(RoleName.ADMIN);
                    Role adminRecruiterRole = roleRepository.findByName(RoleName.RECRUITER);
                    Role adminStaffRole = roleRepository.findByName(RoleName.STAFF);
                    Role adminUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(adminRole);
                    roles.add(adminRecruiterRole);
                    roles.add(adminStaffRole);
                    roles.add(adminUserRole);
                    break;
                case "recruiter":
                    Role recruiterRole = roleRepository.findByName(RoleName.RECRUITER);
                    Role recruiterStaffRole = roleRepository.findByName(RoleName.STAFF);
                    Role recruiterUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(recruiterRole);
                    roles.add(recruiterStaffRole);
                    roles.add(recruiterUserRole);
                    break;
                case "staff":
                    Role staffRole = roleRepository.findByName(RoleName.STAFF);
                    Role staffUserRole = roleRepository.findByName(RoleName.USER);
                    roles.add(staffRole);
                    roles.add(staffUserRole);
                case "user":
                    Role userRole = roleRepository.findByName(RoleName.USER);
                    roles.add(userRole);
                    break;
            }
        }

        User user = User.builder()
            .name(signupRequest.name())
            .username(signupRequest.username())
            .email(signupRequest.email())
            .phoneNumber(signupRequest.phoneNumber())
            .password(passwordEncoder.encode(signupRequest.password()))
            .roles(roles)
            .enabled(true)
            .verificationCode(UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .jobCount(Integer.MAX_VALUE)
            .identityVerified(true)
            .providerType(ProviderType.ADMIN)
            .build();

        User savedUser = userRepository.save(user);

        // kafkaTemplate.send("job-create-user-blockchain", EventCreateUser.builder()
        //     .id(savedUser.getId())
        //     .username(savedUser.getUsername())
        //     .email(savedUser.getEmail())
        //     .phoneNumber(savedUser.getPhoneNumber())
        //     .address(savedUser.getAddress() == null ? "" : savedUser.getAddress())
        //     .enabled(savedUser.isEnabled())
        //     .role(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList()).get(0).name().toLowerCase())
        //     .build()
        // );
        
        return userRepository.save(savedUser);
    }

    @Override
    public ResponseMessage createStaffAccount(StaffAccountRequest staffAccountRequest, String username) {
        if (userRepository.existsByUsername(staffAccountRequest.username())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(staffAccountRequest.email())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (userRepository.existsByPhoneNumber(staffAccountRequest.phoneNumber())) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        if (!staffAccountRequest.password().equals(staffAccountRequest.confirmPassword())) {
            throw new RuntimeException("Mật khẩu không khớp");
        }
        String recruiterCompanyId = userRepository.findByUsername(username).getCompanyId();

        Set<Role> roles = new HashSet<>();
        Role staffRole = roleRepository.findByName(RoleName.STAFF);
        Role staffUserRole = roleRepository.findByName(RoleName.USER);
        roles.add(staffRole);
        roles.add(staffUserRole);

        User user = User.builder()
            .name(staffAccountRequest.name())
            .username(staffAccountRequest.username())
            .email(staffAccountRequest.email())
            .phoneNumber(staffAccountRequest.phoneNumber())
            .password(passwordEncoder.encode(staffAccountRequest.password()))
            .roles(roles)
            .enabled(true)
            .verificationCode(UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .jobCount(Integer.MAX_VALUE)
            .gender(Gender.valueOf(staffAccountRequest.gender().toUpperCase()))
            .dob(staffAccountRequest.dob())
            .address(staffAccountRequest.address())
            .providerType(ProviderType.COMPANY)
            .companyId(recruiterCompanyId)
            .identityVerified(true)
            .build();

        User savedUser = userRepository.save(user);

        // kafkaTemplate.send("job-create-user-blockchain", EventCreateUser.builder()
        //     .id(savedUser.getId())
        //     .username(savedUser.getUsername())
        //     .email(savedUser.getEmail())
        //     .phoneNumber(savedUser.getPhoneNumber())
        //     .address(savedUser.getAddress() == null ? "" : savedUser.getAddress())
        //     .enabled(savedUser.isEnabled())
        //     .role(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toList()).get(0).name().toLowerCase())
        //     .build()
        // );
        
        return ResponseMessage.builder()
                .status(200)
                .message("Tạo tài khoản nhân viên thành công")
                .build();
    }
}
