package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Admins {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)  // Гарантирует, что tgId будет уникальным
    private Long tgId;
}
