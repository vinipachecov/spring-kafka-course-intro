package com.learnkafka.jpa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.jpa.LibraryEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class LibraryEventService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private LibraryEventsRepository libraryEventsRepository;

    public void processLibraryEvent(ConsumerRecord<Integer,String> record) throws JsonProcessingException {
        LibraryEvent libraryEvent =  objectMapper.readValue(record.value(), LibraryEvent.class);
        log.info("libraryEvent {}: ", libraryEvent);

        if (libraryEvent.getLibraryEventId() != null && libraryEvent.getLibraryEventId() == 000) {
            throw new RecoverableDataAccessException("Temporary network issue");
        }

        switch (libraryEvent.getType()) {
            case NEW:
//                save operation
                save(libraryEvent);
                break;
            case UPDATE:
//               validate the libraryEvent
                validate(libraryEvent);
//                update operation
                save(libraryEvent);
                break;
            default:
                log.info("Invalid library event type");
        }

    }

    private void validate(LibraryEvent libraryEvent) {
        if (libraryEvent.getLibraryEventId() == null) throw new IllegalArgumentException("LibraryE event id is missing");

        Optional<LibraryEvent> libraryEventOptional = libraryEventsRepository.findById(libraryEvent.getLibraryEventId());
        if (!libraryEventOptional.isPresent()) throw new IllegalArgumentException(+ libraryEvent.getLibraryEventId() + " Not a valid library event " );

        log.info("Validation is successful for the library event: {}", libraryEventOptional.get());
    }

    private void save(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventsRepository.save(libraryEvent);
        log.info("Successfully persisted the library event {}", libraryEvent);
    }

    public void handleRecovery(ConsumerRecord<Integer, String> consumerRecord) {
        Integer key = consumerRecord.key();
        String message = consumerRecord.value();
        kafkaTemplate.sendDefault(key, message);
    }
}
