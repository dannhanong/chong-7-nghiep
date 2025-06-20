package com.dan.job_service.controllers;

import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.services.HashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job/jobs") // Có thể bạn muốn đổi thành /hashtags
@RequiredArgsConstructor
public class HashtagController {
    private final HashtagService hashtagService;
    // private final JwtService jwtService; // Tạm thời chưa dùng đến

    @GetMapping("/private/hashtags")
    public ResponseEntity<?> searchHashtags(
        @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        try {
            // Gọi phương thức searchHashtags mới từ service và truyền keyword vào
            List<String> hashtags = hashtagService.searchHashtags(keyword);
            return ResponseEntity.ok(hashtags);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseMessage(400, "Lỗi tìm kiếm hashtag: " + e.getMessage()));
        }
    }
}
