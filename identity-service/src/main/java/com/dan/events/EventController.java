package com.dan.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.dan.events.dtos.EventAddSignature;
import com.dan.events.dtos.EventCompanyCreation;
import com.dan.events.dtos.OcrTestMessage;
import com.dan.identity_service.services.AccountService;
import com.dan.identity_service.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;

    @KafkaListener(topics = "job_plus_job_count")
    public void listenJobPlusJobCount(String message) {
        userService.plusJobCount(message);
    }

    @KafkaListener(topics = "job_minus_job_count")
    public void listenJobMinusJobCount(String message) {
        userService.minusJobCount(message);
    }

    @KafkaListener(topics = "job_company_create")
    public void listenJobCompanyCreate(@Payload EventCompanyCreation event,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String companyId) {    
    userService.updateCompanyIdForOwner(event.getOwnerId(), event.getCompanyId());
    }

    @KafkaListener(topics = "ocr_test")
    public void listenOcrTest(Object message) {
        System.out.println("DEBUG: Received message type: " + message.getClass().getName());
        System.out.println("DEBUG: Message content: " + message);
        
        if (message instanceof OcrTestMessage) {
            OcrTestMessage ocrMessage = (OcrTestMessage) message;
            System.out.println("Received username: " + ocrMessage.getUsername());
        } else if (message instanceof String) {
            // Trường hợp nhận được String
            try {
                ObjectMapper mapper = new ObjectMapper();
                OcrTestMessage ocrMessage = mapper.readValue((String)message, OcrTestMessage.class);
                System.out.println("Parsed username: " + ocrMessage.getUsername());
            } catch (Exception e) {
                System.out.println("Raw string: " + message);
            }
        } else {
            System.out.println("Unknown message type");
        }
    }
}
