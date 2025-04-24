//package io.project.clientkeeperbot.client;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.bots.DefaultAbsSender;
//import org.telegram.telegrambots.bots.DefaultBotOptions;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
//import org.telegram.telegrambots.meta.api.objects.Voice;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//
//@Component
//public class TelegramClientImpl extends DefaultAbsSender implements TelegramClient {
//
//    protected TelegramClientImpl(@Value("${bot.token}") String botToken) {
//        super(new DefaultBotOptions(), botToken);
//    }
//
//    @Override
//    public void sendTextMessage(SendMessage message) throws TelegramApiException {
//        super.execute(message);
//    }
//
//    @Override
//    public void sendPhotoMessage(SendPhoto photo) throws TelegramApiException {
//        super.execute(photo);
//    }
//
//
//}