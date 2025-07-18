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
public class RecentActivityRevenueMessage {
    private String userId;
    private String userName;
    private String jobId;
    private long revenue;
}
