package com.dan.identity_service.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dan.identity_service.dtos.enums.RoleName;
import com.dan.identity_service.models.Role;
import com.dan.identity_service.repositories.RoleRepository;
import com.dan.identity_service.repositories.UserRepository;

@Configuration
public class InitDatabase {
    @Bean
    CommandLineRunner init(RoleRepository roleRepository, UserRepository userRepository) {
        return args -> {
            if (!roleRepository.existsByName(RoleName.ADMIN)) {
                Role adminRole = new Role();
                adminRole.setName(RoleName.ADMIN);
                roleRepository.save(adminRole);
            }
            if (!roleRepository.existsByName(RoleName.RECRUITER)) {
                Role recruiterRole = new Role();
                recruiterRole.setName(RoleName.RECRUITER);
                roleRepository.save(recruiterRole);
            }
            if (!roleRepository.existsByName(RoleName.STAFF)) {
                Role staffRole = new Role();
                staffRole.setName(RoleName.STAFF);
                roleRepository.save(staffRole);
            }
            if (!roleRepository.existsByName(RoleName.USER)) {
                Role userRole = new Role();
                userRole.setName(RoleName.USER);
                roleRepository.save(userRole);
            }
        };
    }
}