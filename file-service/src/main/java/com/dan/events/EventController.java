package com.dan.events;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dan.events.dtos.EventFileUpload;
import com.dan.file_service.services.FileUploadService;

@Component
public class EventController {
    @Autowired
    private FileUploadService fileUploadService;

    @KafkaListener(topics = "job-delete-file")
    public void listenJobDeleteFile(EventFileUpload eventFileUpload) {
        try {
            fileUploadService.deleteFileByFileCode(eventFileUpload.getFileCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
