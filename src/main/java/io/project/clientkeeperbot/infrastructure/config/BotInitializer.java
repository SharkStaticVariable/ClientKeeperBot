//package io.project.clientkeeperbot.infrastructure.config;
//
//import io.project.clientkeeperbot.service.TelegramBot;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//@Component
//@RequiredArgsConstructor
//public class BotInitializer {
//    private final TelegramBot telegramBot;
//
//    @PostConstruct
//    public void init() throws TelegramApiException {
//        // Регистрация уже происходит автоматически через LongPollingUpdateConsumer
//    }
//}