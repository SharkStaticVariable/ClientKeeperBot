package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.BotStateContext;
import io.project.clientkeeperbot.entity.RequestsDraft;
import io.project.clientkeeperbot.state.BotState;
import io.project.clientkeeperbot.repository.BotStateContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotStateContextService {
    private final BotStateContextRepository botStateContextRepository;  // Репозиторий для работы с БД

    /**
     * Получить состояние пользователя по его ID
     * @param userId ID пользователя в Telegram
     * @return состояние пользователя или null, если не найдено
     */
    public BotStateContext getBotStateContext(Long userId) {
        Optional<BotStateContext> botStateContext = botStateContextRepository.findById(userId.toString());
        return botStateContext.orElse(null);  // Если не найдено, возвращаем null
    }


    /**
     * Обновить состояние пользователя в базе данных
     * @param userId ID пользователя
     * @param botState новое состояние пользователя
     */

    public void setBotState(Long userId, BotState botState) {
        Optional<BotStateContext> optionalContext = botStateContextRepository.findById(userId.toString());
        BotStateContext botStateContext;

        if (optionalContext.isPresent()) {
            botStateContext = optionalContext.get();
            botStateContext.setCurrentState(botState);
            botStateContext.setLastUpdated(LocalDateTime.now());
        } else {
            botStateContext = new BotStateContext();
            botStateContext.setUserId(userId.toString());
            botStateContext.setCurrentState(botState);
            botStateContext.setLastUpdated(LocalDateTime.now());
            botStateContext.setDraft(new RequestsDraft()); // инициализируем черновик
        }

        botStateContextRepository.save(botStateContext);
    }

}