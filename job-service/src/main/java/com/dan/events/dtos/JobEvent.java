package com.dan.events.dtos;

import com.dan.job_service.models.Job;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties({"createdAt", "updatedAt", "deletedAt"})
public class JobEvent {
    private String eventType;
    private Job data;
}
