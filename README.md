## Spring boot with Kafka (local)


This code is related to the course [Apache Kafka for Developers using Spring Boot LatestEdition | Udemy] (https://www.udemy.com/course/apache-kafka-for-developers-using-springboot/)

## about Kafka

Kafka is a platform for streaming data. It differs from traditional messaging systems like RabbitMQ mostly because of it's usage of disk logs and it's resiliance based on the zookeper cluster.
With this approach which is used during the course the idea is to give an insight of how kafka can be inserted into a microservices architecture using a event-driven strategy. 

As said by Confluent in their Event Driven pdf about Kafka, the only part of traditional Software Architecture that hasn't changed or moved too much is the database. With Message brokers this was a first attempt to create an intermediate layer between producers and consumers, using the kafka terminology, to make the access to data, specially realtime without hitting so hard our databases while also making it able to scale properly without DB horizontal scalling.

## Project

Build a small library event driven app with a producer and a consumer to practice the fundamentals of kafka.



### Producer

The producer is responsible for creating messages to the kafka cluster. In spring boot kafka pluging we used 3 ways to send a message:

- Asynchronous: KafkaTemplate.sendDefault() returning a ListenableFuture -> which will be handled by a callback in the future
- Asynchronous: KafkaTemplate.send()
- Synchronous: KafkaTemplate.sendDefault() returning a SendResult<K,T> directly without a callback

## Consumer

#### Configuration

- KafkaAutoConfiguration: Class instanciated when spring is booted. It uses the application properties set to configure kafka.
- KafkaProperties: one the classes used by KafkaAutoConfiguration as an argument of @EnableConfigurationProperties to load application.propperties data
to get what is set to kafka. 
- KafkaAnnotationDrivenConfiguration: 
    - @ConditionalOnClass: looks for EnableKafka to configure kafka.        
        - KafkaListenerContainerFactory, which is used by KafkaListener to our consumer function
            -   uses a configurer with all configuration for the listener
        - kafkaConsumerFactory
            - reads all data from application properties to configure kafka listener for us. 
    
    
### Consumer Groups
     
Basically multiple instances of the sam application with the same group id.

#### Rebalance

Change the partition ownership from one consumer to another.       
            
#### Commiting offsets
If a consumer reads a message and it goes down, when the it comes up again, it will not 
read again the same message due to the offsets.