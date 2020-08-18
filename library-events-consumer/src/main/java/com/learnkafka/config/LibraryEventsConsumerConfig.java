package com.learnkafka.config;

import javassist.Loader;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {

    private final KafkaProperties properties;

    public LibraryEventsConsumerConfig(KafkaProperties properties) {
        this.properties = properties;
    }

    //     Configuration for kafka listener
    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory
                .getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(this.properties.buildConsumerProperties())));
//      spawn 3 threads for consumer instances
        factory.setConcurrency(3);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setErrorHandler((((thrownException, data) -> {
            log.info("Exception in consumrConfig is {} and the record is {}", thrownException.getMessage(), data);
        })));
        factory.setRetryTemplate(retryTemplate());

        return factory;
    }

    /**
     * Generates a policy that retries at max 3 times
     * with 1 second delay between each attempt
     * @return
     */
    private RetryTemplate retryTemplate() {
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000);
        RetryTemplate retryTemplate =  new RetryTemplate();
        retryTemplate.setRetryPolicy(simpleRetryPolicy());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        return retryTemplate;
    }

    private RetryPolicy simpleRetryPolicy() {
//        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
//        simpleRetryPolicy.setMaxAttempts(3);
        Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
        exceptionsMap.put(IllegalArgumentException.class, false);
        exceptionsMap.put(RecoverableDataAccessException.class, true);
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3,exceptionsMap, true );
        return simpleRetryPolicy;
    }
}


