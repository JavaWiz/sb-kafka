package com.javawiz.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javawiz.consumer.model.Order;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfigurer {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return props;
    }

    public <V> ConsumerFactory<String, V> consumerFactoryForObject(Class<V> clazz) {
        Map<String, Object> props = new HashMap(this.kafkaProperties.buildConsumerProperties());
        ObjectMapper om = new ObjectMapper();
        JavaType type = om.getTypeFactory().constructType(clazz);
        return new DefaultKafkaConsumerFactory(props, new StringDeserializer(), new JsonDeserializer(type, om, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> orderListenerContainerFactory() {
        Map<String, Object> props = new HashMap(this.kafkaProperties.buildConsumerProperties());
        ConcurrentKafkaListenerContainerFactory<String, Order> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryForObject(Order.class));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, List<Order>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<Order>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryForCollection(Order.class));
        return factory;
    }

    public <V> ConsumerFactory<String, List<V>> consumerFactoryForCollection(Class<V> clazz) {
        Map<String, Object> props = new HashMap(this.kafkaProperties.buildConsumerProperties());
        props.put("spring.json.trusted.packages", "*");
        ObjectMapper om = new ObjectMapper();
        JavaType type = om.getTypeFactory().constructParametricType(List.class, new Class[]{clazz});
        return new DefaultKafkaConsumerFactory(props, new StringDeserializer(), new JsonDeserializer(type, om, false));
    }

    /*@Bean
    public RecordMessageConverter converter() {
        return new JsonMessageConverter();
    }*/
}