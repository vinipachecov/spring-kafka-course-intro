package com.learnkafka.domain;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class Book {

    @Id
    private Integer id;

    private String name;

    private String author;

    @OneToOne
    @JoinColumn(name = "libraryEventId")
    @ToString.Exclude
    private LibraryEvent libraryEvent;
}
