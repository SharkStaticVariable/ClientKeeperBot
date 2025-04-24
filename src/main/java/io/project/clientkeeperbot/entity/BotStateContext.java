package io.project.clientkeeperbot.entity;

import io.project.clientkeeperbot.state.BotState;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bot_state_context")
@Data
public class BotStateContext {
    @Id
    private String userId;

    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] stateContext;

    @Enumerated(EnumType.STRING)
    private BotState currentState;

    private LocalDateTime lastUpdated;
    // + getters/setters
}
