package com.dan.job_profile_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableMongoAuditing
public class JobProfileServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobProfileServiceApplication.class, args);
	}

}
