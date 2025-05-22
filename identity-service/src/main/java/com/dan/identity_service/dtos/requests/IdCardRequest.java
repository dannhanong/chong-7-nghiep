package com.dan.identity_service.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdCardRequest {
    String name;
    String dob;
    String gender;
    String nationality;
    String country;
    String address;
    String id_number;
    String expiry_date;
}
