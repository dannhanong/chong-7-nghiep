package com.dan.job_service.controllers;

import com.dan.job_service.dtos.requets.JobRequest;
import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.security.jwt.JwtService;
import com.dan.job_service.services.JobBookmarkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job/bookmarks")
@RequiredArgsConstructor
public class JobBookmarkController {
    private final JobBookmarkService jobBookmarkService;
    private final JwtService jwtService;

    @PostMapping("/public/create/{jobId}")
    public ResponseEntity<ResponseMessage> saveJobBookMark(@PathVariable String jobId, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobBookmarkService.createBookmark(jobId, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Lưu trữ công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi lưu trữ công việc: " + e.getMessage()));
        }
    }

    @DeleteMapping("/public/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteJobBookMark(@PathVariable String id, HttpServletRequest request) {
        try {
            String username = jwtService.getUsernameFromRequest(request);
            jobBookmarkService.deleteBookmark(id, username);
            return ResponseEntity.ok(new ResponseMessage(200, "Xóa lưu trữ công việc thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi khi xóa lưu trữ công việc: " + e.getMessage()));
        }
    }

    @GetMapping("/public/get-all")
    public ResponseEntity<?> getAllBookMark(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        String username = jwtService.getUsernameFromRequest(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(jobBookmarkService.getBookmarks(username, pageable));
    }
}
