package com.dan.file_service.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Document(collection = "file_uploads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUpload {
    @Id
    private String id;
    private String fileName;
    private String fileType;
    private String fileCode;
    private Long size;
}