package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class ExcelExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId; // Telegram ID администратора

    private LocalDateTime generatedAt;

    private LocalDate fromDate;
    private LocalDate toDate;

    private String fileName;

    @Lob
    private byte[] fileData;

    // getters/setters
}
