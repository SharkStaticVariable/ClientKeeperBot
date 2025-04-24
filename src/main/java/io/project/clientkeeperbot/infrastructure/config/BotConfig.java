package io.project.clientkeeperbot.infrastructure.config;

import io.project.clientkeeperbot.service.TelegramBot;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Getter
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

//    @Bean
//    public TelegramBotsLongPollingApplication botsApplication() {
//        return new TelegramBotsLongPollingApplication();
//    }
//@Bean
//public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
//    return new TelegramBotsApi(DefaultBotSession.class);
//}
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot); // Регистрируем бота
        return api;
    }

}