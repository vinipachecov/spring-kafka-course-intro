package com.learnkafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.jpa.LibraryEventsRepository;
import com.learnkafka.jpa.service.LibraryEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssumptions.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"},  partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class LibraryEventsConsumerIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    LibraryEventService libraryEventServiceSpy;

    @Autowired
    LibraryEventsRepository libraryEventsRepository;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Make sure we setup the producer before we start our tests
     */
    @BeforeEach
    void setUp() {
        for (MessageListenerContainer messageListenerContainer: endpointRegistry.getAllListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown() {
        libraryEventsRepository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
////        given
        String json =  "{\"libraryEventId\":null,\"type\":\"NEW\",\"book\":{\"id\":1,\"name\":\"book-name\",\"author\":\"author\"}}";
        kafkaTemplate.sendDefault(json).get();
////        when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
////        then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEventList = (List<LibraryEvent>) libraryEventsRepository.findAll();
        assert libraryEventList.size() == 1;
        libraryEventList.forEach(libraryEvent -> {
            assert libraryEvent.getLibraryEventId()!= null;
            assertEquals(1,libraryEvent.getBook().getId());
        });
    }

    @Test
    void publishUpdateLibraryevent() throws JsonProcessingException, ExecutionException, InterruptedException {
        //    given
        String json =  "{\"libraryEventId\":null,\"type\":\"NEW\",\"book\":{\"id\":1,\"name\":\"book-name\",\"author\":\"author\"}}";
        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        //    save the new LibraryEvent
        libraryEventsRepository.save(libraryEvent);
        Book updatedBook = Book.builder().id(1).name("book-name").author("auitor").build();
        libraryEvent.setType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);
        String updatedjson = objectMapper.writeValueAsString(libraryEvent);
        //    publish the update libraryevent
//       when
        kafkaTemplate.sendDefault(libraryEvent.getLibraryEventId(), updatedjson).get();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

//        then
        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        LibraryEvent processedLibraryEvent = libraryEventsRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("book-name", processedLibraryEvent.getBook().getName());
    }

    @Test
    @DisplayName("ntegration test for the Modify Library Event that's going to result in \"IllegalArgumentException(\"Not a valid library Event\")\"")
    void updateLibraryEventNullId() throws InterruptedException, JsonProcessingException, ExecutionException {
        //    given
        String json =  "{\"libraryEventId\":null,\"type\":\"UPDATE\",\"book\":{\"id\":1,\"name\":\"book-name\",\"author\":\"author\"}}";
        kafkaTemplate.sendDefault(json).get();
////        when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
    }

    @Test
    void publishModifyLibraryEvent_Not_A_Valid_LibraryEventId() throws JsonProcessingException, InterruptedException, ExecutionException {
        //given
        Integer libraryEventId = 123;
        String json = "{\"libraryEventId\":" + libraryEventId + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
        System.out.println(json);
        kafkaTemplate.sendDefault(libraryEventId, json).get();
        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);


        verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        Optional<LibraryEvent> libraryEventOptional = libraryEventsRepository.findById(libraryEventId);
        assertFalse(libraryEventOptional.isPresent());
    }
}
