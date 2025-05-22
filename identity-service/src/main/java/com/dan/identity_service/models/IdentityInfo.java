package com.dan.identity_service.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dan.identity_service.dtos.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "identity_infos")
@CompoundIndexes({
        @CompoundIndex(name = "id_number_index", unique = true, def = "{'idNumber': 1}"),
        @CompoundIndex(name = "user_id_index", def = "{'userId': 1}"),
        @CompoundIndex(name = "deleted_at_index", def = "{'deletedAt': 1}"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class IdentityInfo {
    @Id
    String id;
    String name;
    LocalDate dob;
    Gender gender;
    String nationality;
    String country;
    String address;
    String idNumber;
    LocalDate expiryDate;
    String userId;
    String frontImageCode;
    String backImageCode;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
