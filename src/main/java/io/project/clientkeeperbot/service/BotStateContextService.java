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


    /**
     * Обновить черновик заявки пользователя
     * @param userId ID пользователя
     * @param updateTicketDraft Лямбда-функция для обновления черновика
     */
    public void updateTicketDraft(Long userId, TicketDraftUpdateFunction updateTicketDraft) {
        BotStateContext botStateContext = getBotStateContext(userId);

        if (botStateContext == null) {
            // Если состояния нет, создать новое состояние
            botStateContext = new BotStateContext();
            botStateContext.setUserId(userId.toString());
            botStateContext.setCurrentState(BotState.READY);
            botStateContext.setLastUpdated(LocalDateTime.now());
            botStateContext.setDraft(new RequestsDraft());  // Инициализируем черновик
        }

        // Обновляем черновик
        RequestsDraft draft = botStateContext.getDraft();
        updateTicketDraft.update(draft);

        botStateContext.setDraft(draft);  // Сохраняем обновленный черновик
        botStateContextRepository.save(botStateContext);
    }

    /**
     * Уведомить администратора о новой заявке
     * (Эту логику нужно реализовать в отдельном методе, который будет отправлять сообщение)
     * @param userId ID пользователя, создавшего заявку
     */
    public void notifyAdmin(Long userId) {
        // Пример логики уведомления администратора
        // Здесь можно использовать Telegram API для отправки сообщения администратору
        String adminMessage = "Новая заявка от пользователя " + userId;
        // sendMessageToAdmin(adminMessage);  // Эта функция будет отправлять уведомление админу
    }

    @FunctionalInterface
    public interface TicketDraftUpdateFunction {
        void update(RequestsDraft ticketDraft);
    }
}
