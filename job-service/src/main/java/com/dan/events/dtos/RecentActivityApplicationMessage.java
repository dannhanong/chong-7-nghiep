package com.dan.events.dtos;

import com.dan.job_service.dtos.responses.JobDetail;
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
public class RecentActivityApplicationMessage {
    private JobDetail job;
    private UserDetailToCreateJob user;
}
