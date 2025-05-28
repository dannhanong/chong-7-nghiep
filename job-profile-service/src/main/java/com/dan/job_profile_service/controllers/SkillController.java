package com.dan.job_profile_service.controllers;

import com.dan.job_profile_service.dtos.requests.ProfileRequest;
import com.dan.job_profile_service.dtos.requests.SkillRequest;
import com.dan.job_profile_service.dtos.responses.ResponseMessage;
import com.dan.job_profile_service.models.Skill;
import com.dan.job_profile_service.services.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/jp/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @PostMapping(
            path = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage> createSkill(
            @Valid @ModelAttribute SkillRequest skillRequest) {
        try {
            skillService.create(skillRequest);
            return ResponseEntity.ok(new ResponseMessage(200, "Thêm kỹ năng mới thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi thêm kỹ năng: " + e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSkillById(@PathVariable String id) {
        try {
            Skill skill = skillService.getSkillById(id);
            return ResponseEntity.ok(skill);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ResponseMessage(400, "Không tìm thấy kỹ năng: " + e.getMessage()));
        }
    }

    @PutMapping(
            path = "/update/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseMessage> updateSkill(
            @Valid @ModelAttribute SkillRequest skillRequest,
            @PathVariable String id
    ) {
        try {
            skillService.update(skillRequest, id);
            return ResponseEntity.ok(new ResponseMessage(200, "Chỉnh sửa kỹ năng thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(500, "Lỗi khi sửa kỹ năng: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseMessage> deleteSkill(@PathVariable String id) {
        return ResponseEntity.ok(skillService.delete(id));
    }
}
