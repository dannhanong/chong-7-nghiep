package com.dan.events.dtos;

import com.dan.job_service.dtos.responses.UserDetailToCreateJob;

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
public class RecentActivityJobMessage {
    private String userId;
    private String userName;
    private String jobId;
}
