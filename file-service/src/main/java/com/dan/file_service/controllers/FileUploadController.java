package com.dan.file_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dan.file_service.models.FileUpload;
import com.dan.file_service.services.FileUploadService;

import java.io.IOException;

@RestController
@RequestMapping("/files")
public class FileUploadController {
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/preview/{fileCode}")
    public ResponseEntity<byte[]> previewImage(@PathVariable("fileCode") String fileCode) throws IOException {
        Resource resource = null;
        String contentType = null;
        try {
            resource = fileUploadService.getFileAsResource(fileCode);
            contentType = resource.getURL().openConnection().getContentType();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        byte[] imageData = StreamUtils.copyToByteArray(resource.getInputStream());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(imageData);
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUpload> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            FileUpload fileUpload = fileUploadService.uploadFile(file);
            return ResponseEntity.ok(fileUpload);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
