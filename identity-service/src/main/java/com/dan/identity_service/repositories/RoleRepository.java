package com.dan.identity_service.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dan.identity_service.dtos.enums.RoleName;
import com.dan.identity_service.models.Role;

@Repository
public interface RoleRepository extends MongoRepository<Role, String>{
    Role findByName(RoleName name);
    boolean existsByName(RoleName name);
}
