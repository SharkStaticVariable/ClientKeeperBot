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

    @Enumerated(EnumType.STRING)
    private BotState currentState;// Состояние пользователя в боте

    private LocalDateTime lastUpdated;// Время последнего обновления состояния

    @Transient // <--- Добавляем это!
    private RequestsDraft draft;
    // + getters/setters
}



