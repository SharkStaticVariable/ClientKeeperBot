package io.project.clientkeeperbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

//    @Bean
//    public TelegramBotsApi telegramBotsApi(TelegramBot bot) throws TelegramApiException {
//        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
//        // Отключаем старый webhook через специальный метод
//        bot.clearWebhook();
//
//        api.registerBot(bot);
//        return api;
//    }

}