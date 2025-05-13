package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class FaqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Lob
    private String answer;

    private LocalDateTime createdAt;

    private boolean visible = true; // для скрытия без удаления

    // (опционально) категория
    private String category;

    // getters/setters
}
