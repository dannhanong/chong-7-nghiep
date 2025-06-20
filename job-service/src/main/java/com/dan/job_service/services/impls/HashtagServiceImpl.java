package com.dan.job_service.services.impls;

import com.dan.job_service.models.Hashtag;
import com.dan.job_service.repositories.HashtagRepository;
import com.dan.job_service.services.HashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu đây là một Spring Service Bean
@RequiredArgsConstructor // Tự động tạo constructor cho các trường final
public class HashtagServiceImpl implements HashtagService {

    // Tiêm (Inject) HashtagRepository để sử dụng
    private final HashtagRepository hashtagRepository;

    @Override
    public List<String> searchHashtags(String keyword) {
        // Gọi phương thức từ repository để tìm kiếm các đối tượng Hashtag
        List<Hashtag> hashtags = hashtagRepository.findByTagContainingIgnoreCase(keyword);

        // để trả về cho controller.
        return hashtags.stream()
                       .map(Hashtag::getTag)
                       .collect(Collectors.toList());
    }
}
