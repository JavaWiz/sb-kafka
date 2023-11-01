package com.javawiz.consumer;

import com.javawiz.consumer.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Consumer {
    @KafkaListener(topics = "javawiz" , groupId = "javawiz_group")
    public void listenWithHeaders(
            @Payload Object message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        log.info("Consumed Message -> {}", message);
        log.info("Received Message: {}, from partition: {}", message, partition);
    }

    @KafkaListener(topics = "javawiz" ,
            groupId = "javawiz_group",
            containerFactory = "orderListenerContainerFactory")
    public void listen(Order order) {
        log.info("Received: " + order);
    }
}