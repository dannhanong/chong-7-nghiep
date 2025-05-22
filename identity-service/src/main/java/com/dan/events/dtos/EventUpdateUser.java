package com.dan.events.dtos;

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
public class EventUpdateUser {
    private String id;
    private String email;
    private String phoneNumber;
    private String address;
    private boolean enabled;
    private String role;
}
