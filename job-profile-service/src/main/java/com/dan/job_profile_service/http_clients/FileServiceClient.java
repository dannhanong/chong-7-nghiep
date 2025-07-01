package com.dan.job_profile_service.http_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.dan.job_profile_service.configs.AuthenticationRequestInterceptor;

import java.util.List;
import java.util.Map;

@FeignClient(name = "file-service", configuration = { AuthenticationRequestInterceptor.class })
public interface FileServiceClient {
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> uploadFile(@RequestPart("file") MultipartFile file);

    @DeleteMapping("/files/delete/code/{fileCode}")
    void deleteFileByFileCode(@PathVariable String fileCode);

    @PostMapping(value = "/files/public/upload-for-job/multiple-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<String> uploadMultipleFilesForJob(@RequestPart("files") List<MultipartFile> files);

}
