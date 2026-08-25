package com.vshalt.consumerservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Entity
@Table(name="logs")
@Getter
@Setter
public class LogEntity {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private long id;

    private String uuid;
    private Instant time;
    private String service;
    private String level;

    @Column(columnDefinition = "text")
    private String message;
}
