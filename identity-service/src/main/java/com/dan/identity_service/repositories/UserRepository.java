package com.dan.identity_service.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dan.identity_service.models.Role;
import com.dan.identity_service.models.User;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailAndUsernameNot(String email, String username);
    boolean existsByPhoneNumberAndUsernameNot(String phoneNumber, String username);

    User findByUsername(String username);
    User findByEmail(String email);
    User findByResetPasswordToken(String resetPasswordToken);

    @Query("{ $and: [ { 'deletedAt': null }, { $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'username': { $regex: ?0, $options: 'i' } }, { 'email': { $regex: ?0, $options: 'i' } } ] } ] }")
    Page<User> searchByKeyword(String keyword, Pageable pageable);

    User findByVerificationCode(String code);

    @Transactional
    @Query("{ '_id': ?0 }")
    void enableUser(String id);

    List<User> findAllByCreatedAtBeforeAndEnabled(Instant twoDaysAgo, boolean enabled);
    List<User> findByRoles(Role role);
}
