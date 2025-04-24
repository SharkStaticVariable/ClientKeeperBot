package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.Users;
import io.project.clientkeeperbot.infrastructure.config.BotConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final UserService userService;
    private final CaptchaService captchaService;
    private final BotConfig botConfig;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        try {
            if (text.equals("/start")) {
                handleStartCommand(chatId, update);
            } else if (captchaService.isCaptchaPending(chatId)) {
                handleCaptchaInput(chatId, text, update.getMessage().getFrom());
            }
        } catch (Exception e) {
            handleError(chatId, e);
        }
    }

    private void handleStartCommand(Long chatId, Update update) throws TelegramApiException {
        User telegramUser = update.getMessage().getFrom();

        String welcomeMessage = String.format(
                "Привет, %s! 👋\nДобро пожаловать! Для начала работы пройдите проверку безопасности.",
                telegramUser.getFirstName()
        );

        sendMessage(chatId, welcomeMessage);
        sendCaptcha(chatId);

        // Сохраняем информацию для последующей проверки
        captchaService.storeUserData(chatId, telegramUser);
    }

    private void handleCaptchaInput(Long chatId, String inputText, User telegramUser) throws TelegramApiException {
        if (captchaService.verifyCaptcha(chatId, inputText)) {
            // Только после успешной проверки сохраняем пользователя
            userService.registerUser(telegramUser);
            sendSuccessMessage(chatId);
            sendMainMenu(chatId);
        } else {
            sendMessage(chatId, "Неверный код капчи. Попробуйте еще раз.");
            sendCaptcha(chatId);
        }
    }

    private void sendSuccessMessage(Long chatId) throws TelegramApiException {
        sendMessage(chatId, "✅ Проверка пройдена успешно! Теперь вы можете пользоваться ботом.");
    }

    private void sendMainMenu(Long chatId) throws TelegramApiException {
        SendMessage menu = new SendMessage();
        menu.setChatId(chatId.toString());
        menu.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(List.of(
                new KeyboardRow(List.of(new KeyboardButton("FAQ"))),
                new KeyboardRow(List.of(new KeyboardButton("Оставить отзыв"))),
                new KeyboardRow(List.of(new KeyboardButton("Создать заявку")))
        ));

        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        menu.setReplyMarkup(keyboard);

        execute(menu);
    }

    private void sendCaptcha(Long chatId) throws TelegramApiException {
        try {
            String imageBase64 = captchaService.generateCaptchaImageBase64(chatId);
            SendPhoto photo = new SendPhoto(
                    chatId.toString(),
                    new InputFile(
                            new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64)),
                            "captcha.png"
                    )
            );
            photo.setCaption("Введите код с картинки для подтверждения:");
            execute(photo);
        } catch (IOException e) {
            throw new TelegramApiException("Ошибка генерации капчи", e);
        }
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        execute(new SendMessage(chatId.toString(), text));
    }

    private void handleError(Long chatId, Exception e) {
        try {
            sendMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, попробуйте позже.");
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
        e.printStackTrace();
    }
}