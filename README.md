# Spring Boot With Kafka

## Install Kafka using Docker
Kafka is a JVM application and we can run it on many operating systems such as Windows, macOS, Linux by just installing JRE (you can install JDK instead). But what do we need to know about Kafka to install it?

## Apache ZooKeeper
Even if we want to run a single broker Kafka installation or a huge cluster, we should install Apache ZooKeeper. For test purposes, we can install a standalone ZooKeeper server but for production, you should set up a Zookeeper cluster.
   
Kafka stores information about the cluster and consumers into Zookeeper. ZooKeeper acts as a coordinator between them.

In Kafka jargon, a single Kafka server (process) is called a broker. The main responsibilities of a broker are: get messages from producers, store them on disk and respond to consumer's requests.

A Kafka broker can be a part of a Kafka cluster. A broker can be a cluster controller or not and also be a leader (owner) of several partitions and keeps a redundant copy of several other partitions (for replication).

Here we install Zookeeper and Kafka in docker container.

## Why Docker
Docker has been a revolutionary technology for developers, testers, and deployers. As a developer, docker helps me to have a clean OS without the need to install anything on it directly. On the other hand, installing some systems or servers are so hard because of installation complexity and dependencies, Kafka is one of those systems that have a dependency on ZooKeeper and also has some prerequisites which I don't want to set them every time. By using Docker and Docker Compose we can have a single file that allows us to run a testing single Kafka broker or a cluster.

I use [Bitnami](https://bitnami.com/stack/kafka) images for creating containers for Kafka and ZooKeeper:

## Kafka
About Bitnami Kafka Stack Kafka is a distributed streaming platform designed to build real-time pipelines.
Bitnami image for Kafka contains the latest bug fixes and features of Kafka and based on minideb which is a minimalist Debian based container image (a small base container image).
As we said, in any case, if you want to install and run Kafka you should run a ZooKeeper server. Before running ZooKeeper, container using docker, we create a docker network for our cluster:
```
docker network create kafka-net --driver bridge
```
Now we should run a ZooKeeper container from Bitnami ZooKeeper image:
```
docker run --name zookeeper-server -p 2181:2181 --network kafka-net -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:latest
```
By default, ZooKeeper runs on port 2181 and we expose that port using -p param so that we can access ZooKeeper from our client application on the test host machine. We set “ALLOW_ANONYMOUS_LOGIN” configuration to true because we want to allow users to connect to ZooKeeper easily (without authentication), this configuration is not suitable for production use.

Now everything is ready to run our first broker and join it to the cluster. In order to add a broker to the cluster, we need to introduce it to ZooKeeper. A Kafka broker introduces itself to the Zookeeper server (or cluster) by knowing about the Zookeeper server IP address:
```
docker run --name kafka-server --network kafka-net -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper-server:2181 -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -p 9092:9092 bitnami/kafka:latest
```
We send the Zookeeper server address and port using the “KAFKA_CFG_ZOOKEEPER_CONNECT” configuration and by setting “ALLOW_PLAINTEXT_LISTENER” configuration to false, allow plain text communication (for example instead of SSL) and also set “KAFKA_CFG_ADVERTISED_LISTENERS” configuration value so that ZooKeeper can publish a public IP address and port for this broker to clients.
Now we have a single broker Kafka cluster and we can work and test it.

## Use docker-compose instead of several docker commands
Instead of running several docker commands to create a network and run a container for ZooKeeper and Kafka brokers, you can use Docker Compose to set up your cluster more easily. Since docker-compose automatically sets up a new network and attaches all deployed services to that network, you don't need to define kafka-net network explicitly:
```
version: '2'

networks:
  kafka-net:
    driver: bridge

services:
  zookeeper-server:
    image: 'bitnami/zookeeper:latest'
    networks:
      - kafka-net
    ports:
      - '2181:2181'
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
  kafka-server1:
    image: 'bitnami/kafka:latest'
    networks:
      - kafka-net    
    ports:
      - '9092:9092'
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper-server:2181
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - ALLOW_PLAINTEXT_LISTENER=yes
    depends_on:
      - zookeeper-server
  kafka-server2:
    image: 'bitnami/kafka:latest'
    networks:
      - kafka-net    
    ports:
      - '9093:9092'
    environment:
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper-server:2181
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9093
      - ALLOW_PLAINTEXT_LISTENER=yes
    depends_on:
      - zookeeper-server
```
We can save this in a file docker-compose.yml. Now you can run your cluster by executing just one command in our project root directory:
```
docker-compose up -d
```
Wait for some minutes and then you can connect to the Kafka cluster using from our app. If you want to add a new Kafka broker to this cluster in the future, you can use previous docker run commands.

Let's start our spring boot setup. We need to add the spring-kafka dependency to our pom.xml:
```
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
## Configuring Topics

PrevPreviously we used to run command line tools to create topics in Kafka such as:
```
kafka-topics.sh --create zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic javawiz
```
But with the introduction of AdminClient in Kafka, we can now create topics programmatically.

We need to add the KafkaAdmin Spring bean, which will automatically add topics for all beans of type NewTopic:
```
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfigurer {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapAddress;
 
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }
    
    @Bean
    public NewTopic topic1() {
         return new NewTopic("javawiz", 1, (short) 1);
    }
}
```
## Producing Messages
To create messages, first, we need to configure a ProducerFactory which sets the strategy for creating Kafka Producer instances.

Then we need a KafkaTemplate which wraps a Producer instance and provides convenience methods for sending messages to Kafka topics.

Producer instances are thread-safe and hence using a single instance throughout an application context will give higher performance. Consequently, KakfaTemplate instances are also thread-safe and use of one instance is recommended.
```
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfigurer {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
 
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```
## Publishing Messages
We can send messages using the KafkaTemplate class:

```
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
        log.info(String.format("Producing message --> %s", message));
        kafkaTemplate.send(TOPIC, message);
    }
}
```
## Consumer Configuration
For consuming messages, we need to configure a ConsumerFactory and a KafkaListenerContainerFactory. Once these beans are available in the Spring bean factory, POJO based consumers can be configured using @KafkaListener annotation.

@EnableKafka annotation is required on the configuration class to enable detection of @KafkaListener annotation on spring managed beans:
```
import com.javawiz.model.Order;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class kafkaConsumerConfigurer {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "json");

        return props;
    }

    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                new JsonDeserializer<>(Order.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}

```
## Consuming Messages
Spring also supports retrieval of one or more message headers using the @Header annotation in the listener:
```

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Consumer {
    @KafkaListener(topics = "javawiz", groupId = "javawiz_group")
    public void consume(Object message) {
        log.info("Consumed Message -> {}", message);
    }

    @KafkaListener(topics = "javawiz" , groupId = "javawiz_group")
    public void listenWithHeaders(
            @Payload Object message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        log.info("Consumed Message -> {}", message);
        log.info("Received Message: {}, from partition: {}", message, partition);
    }
}
```
