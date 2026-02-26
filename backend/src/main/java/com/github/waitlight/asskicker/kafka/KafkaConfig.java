package com.github.waitlight.asskicker.kafka;

import com.github.waitlight.asskicker.model.SendTask;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String SEND_TASKS_TOPIC = "ass-kicker.send-tasks";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public NewTopic sendTasksTopic() {
        return TopicBuilder.name(SEND_TASKS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, SendTask> sendTaskProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, SendTask> kafkaTemplate(ProducerFactory<String, SendTask> sendTaskProducerFactory) {
        return new KafkaTemplate<>(sendTaskProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, SendTask> sendTaskConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "ass-kicker");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.github.waitlight.asskicker.model");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SendTask.class.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SendTask> sendTaskListenerContainerFactory(
            ConsumerFactory<String, SendTask> sendTaskConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, SendTask> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(sendTaskConsumerFactory);
        return factory;
    }
}
