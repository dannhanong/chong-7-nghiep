package com.dan.job_profile_service.controllers;

import com.dan.job_profile_service.dtos.requests.ProfileRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/jp/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping(
            path = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage> createProfile(
            @Valid @ModelAttribute ProfileRequest profileRequest
    ) {
        try {
            profileService.create(profileRequest);
            return ResponseEntity.ok(new ResponseMessage(200, "Tạo hồ sơ thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi tạo hồ sơ: " + e.getMessage()));
        }
    }

    @GetMapping("/get-full-profile/{id}")
    public ResponseEntity<?> getFullProfileById(@PathVariable String id){
        try {
            return ResponseEntity.ok(profileService.getFullProfileById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("")
    public ResponseEntity<?> getAllProfiles(){
        try {
            return ResponseEntity.ok(profileService.getAllProfiles());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getProfileById(@Valid @PathVariable String id){
        try {
            return ResponseEntity.ok(profileService.getProfileById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Lỗi lấy thông tin hồ sơ: " + e.getMessage()));
        }
    }

    @PutMapping(
            path = "/update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage> updateProfile(@Valid @ModelAttribute ProfileRequest profileRequest, @PathVariable String id) {
        try {
            profileService.update(profileRequest, id);
            return ResponseEntity.ok(new ResponseMessage(200, "Cập nhật hồ sơ thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi cập nhật hồ sơ: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteProfile(@PathVariable String id) {
        return ResponseEntity.ok(profileService.delete(id));
    }
}
