package com.dan.job_profile_service.http_clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "file-service",
            url = "${file.service.url}"
//        configuration = {AuthenticationRequestInterceptor.class},
//        url = "${base.url.file-service}")
)
public interface FileServiceClient {
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> uploadFile(@RequestPart("file") MultipartFile file);

    @DeleteMapping("/files/delete/code/{fileCode}")
    void deleteFileByFileCode(@PathVariable String fileCode);
}
