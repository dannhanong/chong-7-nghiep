package com.dan.job_service.services;

import com.dan.job_service.dtos.requets.SearchClickRequest;
import com.dan.job_service.models.SearchClick;

public interface SearchClickService {
    SearchClick saveSearchClick(SearchClickRequest request, String username);
}
