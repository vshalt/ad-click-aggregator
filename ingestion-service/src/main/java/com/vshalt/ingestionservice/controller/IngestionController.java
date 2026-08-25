package com.vshalt.ingestionservice.controller;

import com.vshalt.ingestionservice.dto.request.Log;
import com.vshalt.ingestionservice.producer.KafkaProducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api/v1/logs")
public class IngestionController {

    @Autowired
    private KafkaProducer producer;

    @PostMapping("/")
    public String createLog(@RequestBody Log request) {
        producer.sendEvent(request);
        return "ok";
    }
}
