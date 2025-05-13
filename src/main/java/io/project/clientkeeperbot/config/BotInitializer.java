package io.project.clientkeeperbot.config;

import io.project.clientkeeperbot.service.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.net.InetAddress;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotInitializer {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        new Thread(this::startBotWithReconnect).start();
    }


    private void startBotWithReconnect() {
        while (true) {
            if (!isTelegramApiAvailable()) {
                log.warn("🚫 Telegram API недоступен. Повтор через 10 секунд...");
                sleep(10);
                continue;
            }

            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBot.clearWebhook();
                botsApi.registerBot(telegramBot);
                log.info("✅ Бот успешно запущен!");
                break;
            } catch (TelegramApiException e) {
                log.error("Ошибка запуска Telegram-бота: {}", e.getMessage());
                sleep(10);
            }
        }
    }

    private boolean isTelegramApiAvailable() {
        try {
            InetAddress address = InetAddress.getByName("api.telegram.org");
            return address.isReachable(5000);
        } catch (IOException e) {
            return false;
        }
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {}
    }
}
