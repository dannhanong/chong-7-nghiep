package com.dan.job_service.services.impls;

import com.dan.job_service.dtos.requets.SearchClickRequest;
import com.dan.job_service.http_clients.IdentityServiceClient;
import com.dan.job_service.models.SearchClick;
import com.dan.job_service.repositories.SearchClickRepository;
import com.dan.job_service.services.SearchClickService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SearchClickServiceImpl implements SearchClickService {
    private final SearchClickRepository searchClickRepository;
    private final IdentityServiceClient identityServiceClient;

    @Override
    public SearchClick saveSearchClick(SearchClickRequest request, String username) {
        String userId = identityServiceClient.getUserByUsername(username).getId();
        SearchClick searchClick = SearchClick.builder()
            .userId(userId)
            .jobId(request.jobId())
            .searchQuery(request.searchQuery())
            .positionInResults(request.positionInResults())
            .timestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now())
            .build();
            
        return searchClickRepository.save(searchClick);
    }
}
