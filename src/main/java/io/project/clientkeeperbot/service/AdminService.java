package io.project.clientkeeperbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdminService extends TelegramLongPollingBot {
    private static final List<Long> ADMIN_IDS = List.of(6180241984L); // Ваш Telegram ID !!!!!!!!!!

    @Override
    public void onUpdateReceived(Update update) {
        if (!ADMIN_IDS.contains(update.getMessage().getChatId())) return;

        if (update.getMessage().getText().equals("/admin")) {
            SendMessage panel = new SendMessage();
            panel.setText("Админ-панель:");

            // Создаем ряды кнопок (каждый ряд - отдельный KeyboardRow)
            List<KeyboardRow> keyboardRows = new ArrayList<>();

            // Первый ряд кнопок
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton("Модерация заявок"));
            row1.add(new KeyboardButton("Добавить FAQ"));

            // Второй ряд кнопок
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton("Создать отчёт"));

            keyboardRows.add(row1);
            keyboardRows.add(row2);

            // Устанавливаем клавиатуру
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setKeyboard(keyboardRows);
            keyboardMarkup.setResizeKeyboard(true); // Автоматическое изменение размера кнопок

            panel.setReplyMarkup(keyboardMarkup);

            try {
                execute(panel);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }
}
