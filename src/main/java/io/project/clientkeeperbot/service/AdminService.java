package io.project.clientkeeperbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminAuthService adminAuthService;

    // Метод для отправки админ-панели с кнопками
    public void showAdminPanel(Long chatId, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Админ-панель: Выберите действие:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первая строка
        InlineKeyboardButton moderationButton = new InlineKeyboardButton();
        moderationButton.setText("Модерация заявок");
        moderationButton.setCallbackData("moderation");

        InlineKeyboardButton faqButton = new InlineKeyboardButton();
        faqButton.setText("Добавление FAQ");
        faqButton.setCallbackData("addfaq");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(moderationButton);
        row1.add(faqButton);

        // Вторая строка
        InlineKeyboardButton reportButton = new InlineKeyboardButton();
        reportButton.setText("Отчетность");
        reportButton.setCallbackData("report");

        InlineKeyboardButton systemButton = new InlineKeyboardButton();
        systemButton.setText("Система");
        systemButton.setCallbackData("system");

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(reportButton);
        row2.add(systemButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        telegramBot.execute(message);
    }

    // Обработка callback для различных кнопок
    public void handleAdminCallback(Long chatId, String callbackData, TelegramBot telegramBot) throws TelegramApiException {
        switch (callbackData) {
            case "moderation":
                sendTextMessage(chatId, "Вы выбрали модерацию заявок.", telegramBot);
                break;
            case "addfaq":
                sendTextMessage(chatId, "Вы выбрали добавление FAQ.", telegramBot);
                break;
            case "report":
                sendTextMessage(chatId, "Вы выбрали отчетность.", telegramBot);
                break;
            case "system":
                showSystemOptions(chatId, telegramBot);
                break;
            case "back_to_admin_panel":
                showAdminPanel(chatId, telegramBot);
                break;
            case "addadmin":
                sendTextMessage(chatId, "Введите Telegram ID для добавления администратора:\nПример: addadmin:123456789", telegramBot);
                break;
            case "removeadmin":
                sendTextMessage(chatId, "Введите Telegram ID для удаления администратора:\nПример: removeadmin:123456789", telegramBot);
                break;
        }
    }

    // Метод для отображения кнопок для управления администраторами
    public void showSystemOptions(Long chatId, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Система: Выберите действие:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первая строка
        InlineKeyboardButton addAdminButton = new InlineKeyboardButton();
        addAdminButton.setText("Добавить администратора");
        addAdminButton.setCallbackData("addadmin");

        InlineKeyboardButton removeAdminButton = new InlineKeyboardButton();
        removeAdminButton.setText("Удалить администратора");
        removeAdminButton.setCallbackData("removeadmin");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(addAdminButton);
        row1.add(removeAdminButton);

        // Вторая строка - кнопка назад
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("back_to_admin_panel");

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        telegramBot.execute(message);
    }

    // Метод для отправки текстового сообщения
    public void sendTextMessage(Long chatId, String text, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        telegramBot.execute(message);
    }

    // Пример для обработки добавления и удаления администраторов
    public void processAdminAction(Long chatId, String text, TelegramBot telegramBot) throws TelegramApiException {
        if (text.startsWith("addadmin:")) {
            String tgIdString = text.substring(9);
            try {
                Long tgId = Long.parseLong(tgIdString);
                adminAuthService.addAdmin(tgId);
                sendTextMessage(chatId, "✅ Новый администратор добавлен.", telegramBot);
            } catch (NumberFormatException e) {
                sendTextMessage(chatId, "❌ Неверный формат Telegram ID.", telegramBot);
            }
        } else if (text.startsWith("removeadmin:")) {
            String tgIdString = text.substring(12);
            try {
                Long tgId = Long.parseLong(tgIdString);
                adminAuthService.removeAdmin(tgId);
                sendTextMessage(chatId, "✅ Администратор удален.", telegramBot);
            } catch (NumberFormatException e) {
                sendTextMessage(chatId, "❌ Неверный формат Telegram ID.", telegramBot);
            }
        }
    }
}
