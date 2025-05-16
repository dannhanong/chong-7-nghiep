package com.dan.job_service.dtos.requets;

public record CategoryRequest(
    String name,
    String description,
    String parentId
) {}
