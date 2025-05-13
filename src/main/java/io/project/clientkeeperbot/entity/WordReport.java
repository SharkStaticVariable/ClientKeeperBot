package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class WordReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Request request;

    @OneToOne
    private AdminResponse response;

    private String fileName;
    private String filePath;

    private LocalDateTime generatedAt;

    private Long adminId;

}