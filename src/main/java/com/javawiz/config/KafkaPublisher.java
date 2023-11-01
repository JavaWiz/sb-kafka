package com.javawiz.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public interface KafkaPublisher {
    String KAFKA_TOPIC_HEADER = "kafka_topic";

    void publish(Object object);

    default Message<Object> objectToMessage(Object object, String topicName) {
        return MessageBuilder.withPayload(object).setHeader("kafka_topic", topicName).build();
    }
}