package com.javawiz.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class Producer {

    private static final String TOPIC = "javawiz";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(Object message) {
        log.info("Producing message --> {}", message);
        kafkaTemplate.send(TOPIC, message);
    }
}