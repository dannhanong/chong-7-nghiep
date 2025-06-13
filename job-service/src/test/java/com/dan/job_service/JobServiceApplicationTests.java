package com.dan.job_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.dan.job_service.dtos.responses.ResponseMessage;
import com.dan.job_service.services.JobService;

@SpringBootTest
class JobServiceApplicationTests {
	@Autowired
	private JobService jobService;

	@Test
	void contextLoads() {
		ResponseMessage responseMessage = jobService.testEmJob();
		System.out.println("Response Message: " + responseMessage.getMessage());
	}

}
