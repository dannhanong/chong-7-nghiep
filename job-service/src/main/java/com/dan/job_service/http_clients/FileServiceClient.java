package com.dan.job_service.http_clients;

import java.util.List;
import java.util.Map;

import com.dan.job_service.configs.AuthenticationRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "file-service", configuration = {AuthenticationRequestInterceptor.class})
public interface FileServiceClient {
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> uploadFile(@RequestPart("file") MultipartFile file);

    @PostMapping(value = "/files/public/upload-for-job/multiple-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<String> uploadMultipleFilesForJob(@RequestPart("files") List<MultipartFile> files);

    @DeleteMapping(value = "/files/delete/code/{fileCode}")
    void deleteFileByFileCode(@PathVariable("fileCode") String fileCode);
}