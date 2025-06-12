package com.dan.job_service.http_clients;

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

    @DeleteMapping(value = "/files/delete/code/{fileCode}")
    void deleteFileByFileCode(@PathVariable("fileCode") String fileCode);
}