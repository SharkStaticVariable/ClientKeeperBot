package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.config.BotConfig;
//import io.project.clientkeeperbot.entity.Attachment;
import io.project.clientkeeperbot.entity.BotStateContext;
import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.RequestsDraft;
import io.project.clientkeeperbot.state.BotState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final UserService userService;
    private final CaptchaService captchaService;
    private final BotConfig botConfig;
    private final AdminAuthService adminAuthService;
    private final AdminService adminService;
    private final BotStateContextService botStateContextService;
    private final RequestService requestService;
    private final RequestDraftMemoryService draftMemoryService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        execute(message);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Received update: " + update); // Добавьте вывод в консоль для отладки
        executorService.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            User telegramUser = update.getMessage().getFrom();
            String messageText = update.getMessage().getText(); //если что убрать

            try {
                // Проверка: если админ
                if (adminAuthService.isAdmin(chatId)) {
                    if (text.equals("/start")) {
                        sendTextMessage(chatId, "Вы администратор. Для управления введите /admin.");
                        return;
                    }
                    adminService.handleAdminText(chatId, messageText, this);
                    handleAdminCommands(chatId, text);
                    return;
                }

                // Если обычный пользователь ввел /admin — отказ
                if (text.equals("/admin")) {
                    sendTextMessage(chatId, "🛑 Данная команда вам недоступна. Введите /start для работы с ботом.");
                    return;
                }

                // Проверка: пользователь зарегистрирован
                boolean isRegistered = userService.isUserRegistered(chatId);

                // Обработка /start
                if (text.equals("/start")) {
                    if (isRegistered) {
                        handleStartCommand2(chatId, update);
                        sendReplyKeyboardWithMenuButton(chatId);
                    } else {
                        botStateContextService.setBotState(chatId, BotState.WAITING_CAPTCHA);
                        handleStartCommand(chatId, update);
                    }
                    return;
                }

                // Пользователь зарегистрирован → обрабатываем его команды
                BotStateContext botStateContext = botStateContextService.getBotStateContext(chatId);
                if (botStateContext == null) {
                    botStateContextService.setBotState(chatId, BotState.START);
                    return;
                }

                BotState currentState = botStateContext.getCurrentState();

                if (currentState == BotState.WAITING_CAPTCHA) {
                    handleCaptchaInput(chatId, text, telegramUser, botStateContext);
                    return;
                }
                handleUserCommands(chatId, text, currentState);
            } catch (Exception e) {
                handleError(chatId, e);
            }
        }

        // Обработка callback-запросов
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            String callbackData = callbackQuery.getData();

            try {
                // Проверка: если админ — обрабатываем отдельным методом
                if (adminAuthService.isAdmin(chatId)) {
                    adminService.handleAdminCallback(chatId, callbackData, this);
                    return;
                }
                // Общая обработка callback для пользователей
                handleCallbackQuery(callbackQuery);
            } catch (Exception e) {
                handleError(chatId, e);
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();

        if (callbackData.startsWith("type:")) {
            String selectedType = callbackData.substring(5);
            requestService.setType(chatId, selectedType);

            if ("Другое".equals(selectedType)) {
                botStateContextService.setBotState(chatId, BotState.ASK_PROJECT_TYPE);
                sendTextMessage(chatId, "Введите свой вариант типа проекта:");
            } else {
                botStateContextService.setBotState(chatId, BotState.ASK_DESCRIPTION);
                sendTextMessage(chatId, "Опишите ваш проект:");
            }
            return;
        }

        switch (callbackData) {
            case "faq" -> sendTextMessage(chatId, "FAQ: Как я могу помочь вам?");
            case "leave_feedback" -> {
                sendTextMessage(chatId, "ваши заявки.");
                sendUserRequests(chatId);
            }
            case "create_request" -> {
                requestService.startDraft(chatId);
                botStateContextService.setBotState(chatId, BotState.ENTER_CUSTOM_PROJECT_TYPE);
                sendProjectTypeOptions(chatId);
            }
            case "edit_type" -> {
                botStateContextService.setBotState(chatId, BotState.EDIT_PROJECT_TYPE);
                sendTextMessage(chatId, "Введите новый тип проекта:");
            }
            case "edit_description" -> {
                botStateContextService.setBotState(chatId, BotState.EDIT_DESCRIPTION);
                sendTextMessage(chatId, "Введите новое описание:");
            }
            case "edit_deadline" -> {
                botStateContextService.setBotState(chatId, BotState.EDIT_DEADLINE);
                sendTextMessage(chatId, "Введите новые сроки:");
            }
            case "edit_budget" -> {
                botStateContextService.setBotState(chatId, BotState.EDIT_BUDGET);
                sendTextMessage(chatId, "Введите новый бюджет:");
            }
            case "edit_contact" -> {
                botStateContextService.setBotState(chatId, BotState.EDIT_CONTACT);
                sendTextMessage(chatId, "Введите новый контакт:");
            }
            case "submit_request" -> {
                requestService.saveFinalRequest(chatId);
                botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
                sendTextMessage(chatId, "✅ Ваша заявка принята, ожидайте ответа.");
                sendMainMenu(chatId);

            }
            case "cancel_request" -> {
                botStateContextService.setBotState(chatId, BotState.CONFIRM_CANCEL);
                sendCancelConfirmation(chatId);
            }
            case "confirm_cancel" -> {
                draftMemoryService.clearDraft(chatId); // Удаляем черновик
                botStateContextService.setBotState(chatId, BotState.MAIN_MENU);

                sendTextMessage(chatId, "❌ Ваша заявка отменена.");
                sendMainMenu(chatId);

            }
            case "cancel_cancel" -> {
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }
            default -> sendTextMessage(chatId, "Неизвестная команда.");
        }

    }

    private void sendCancelConfirmation(Long chatId) throws TelegramApiException {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(button("✅ Да", "confirm_cancel"), button("❌ Нет", "cancel_cancel")));

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Вы уверены, что хотите отменить заявку?");
        message.setReplyMarkup(inlineKeyboard);

        execute(message);
    }

    private void handleStartCommand(Long chatId, Update update) throws TelegramApiException {
    User telegramUser = update.getMessage().getFrom();

    // Получаем имя и фамилию пользователя
    String firstName = telegramUser.getFirstName();
    String lastName = telegramUser.getLastName();

    // Формируем приветственное сообщение
    String welcomeMessage = String.format(
            "Привет, %s %s! 👋\nДобро пожаловать! Для начала работы пройдите проверку безопасности.",
            firstName, (lastName != null ? lastName : "")
    ).trim(); // .trim() удаляет лишний пробел, если фамилия пустая

    sendTextMessage(chatId, welcomeMessage);
    sendCaptcha(chatId);

    BotStateContext botStateContext = botStateContextService.getBotStateContext(chatId);
    if (botStateContext == null) {
        System.out.println("State is null, setting to WAITING_CAPTCHA");
        botStateContextService.setBotState(chatId, BotState.WAITING_CAPTCHA);
    }
    captchaService.storeUserData(chatId, telegramUser);
}

    private void handleStartCommand2(Long chatId, Update update) throws TelegramApiException {
        User telegramUser = update.getMessage().getFrom();

        // Получаем имя и фамилию пользователя
        String firstName = telegramUser.getFirstName();
        String lastName = telegramUser.getLastName();

        // Формируем приветственное сообщение
        String welcomeMessage = String.format(
                "Привет, %s %s! 👋\nДобро пожаловать!",
                firstName, (lastName != null ? lastName : "")
        ).trim(); // .trim() удаляет лишний пробел, если фамилия пустая

        sendTextMessage(chatId, welcomeMessage);
    }

    private void handleCaptchaInput(Long chatId, String inputText, User telegramUser, BotStateContext botStateContext) throws TelegramApiException {
        if (captchaService.verifyCaptcha(chatId, inputText)) {
            userService.registerUser(telegramUser);
            sendSuccessMessage(chatId);
            botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
        } else {
            sendTextMessage(chatId, "Неверный код капчи. Попробуйте еще раз.");
            sendCaptcha(chatId);
        }
    }

    private void handleAdminCommands(Long chatId, String text) throws TelegramApiException {
        if (text.equals("/admin")) {
              adminService.sendReplyKeyboardWithMenuButtonAdmin(chatId, this);
        } else if (text.equals("/start")) {
            sendTextMessage(chatId, "⚠️ Вы уже в админ-панели, если нет, то введите команду /admin");
        }
    }

    private void sendUserRequests(Long chatId) {
        List<Request> userRequests = requestService.getRequestsByClientId(chatId);

        if (userRequests.isEmpty()) {
            try {
                sendTextMessage(chatId, "У вас пока нет заявок.");
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        for (Request req : userRequests) {
            String message = String.format(
                    "🆔 №%d\n" +
                            "📌 Тип: %s\n" +
                            "📝 Описание: %s\n" +
                            "📅 Сроки: %s\n" +
                            "💰 Бюджет: %s\n" +
                            "📱 Контакт: %s\n" +
                            "📊 Статус: %s",
                    req.getId(),
                    req.getType(),
                    req.getDescription(),
                    req.getDeadline(),
                    req.getBudget(),
                    req.getContact(),
                    req.getStatus().getDisplayName()
            );

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton editButton = new InlineKeyboardButton("✏️ Редактировать");
            editButton.setCallbackData("edit_request:" + req.getId());

            rows.add(List.of(editButton));
            markup.setKeyboard(rows);

//            sendTextMessageWithInlineKeyboard(chatId, message, markup);

            // ➡️ Теперь отдельно присылаем прикрепленные файлы
//            for (Attachment attachment : req.getAttachments()) {
//                SendDocument sendDocument = new SendDocument();
//                sendDocument.setChatId(chatId.toString());
//                sendDocument.setDocument(new InputFile(attachment.getFileId()));
//                try {
//                    execute(sendDocument);
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }
//            }

            try {
                sendTextMessageWithInlineKeyboard(chatId, message, markup);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendReplyKeyboardWithMenuButton(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Обратитесь в меню"); // Отправит пробел — Telegram примет

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("📋 Меню"));
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        markup.setOneTimeKeyboard(false); // чтобы кнопка не исчезала

        message.setReplyMarkup(markup);

        execute(message);
    }

    private static final Pattern TELEGRAM_USERNAME_PATTERN = Pattern.compile("^@?[a-zA-Z0-9_]{5,32}$");

    public boolean isValidTelegramUsername(String input) {
        return TELEGRAM_USERNAME_PATTERN.matcher(input).matches();
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    public boolean isValidEmail(String input) {
        return EMAIL_PATTERN.matcher(input).matches();
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    public boolean isValidPhone(String input) {
        return PHONE_PATTERN.matcher(input).matches();
    }
    public boolean isValidContact(String input) {
        return isValidPhone(input) || isValidEmail(input) || isValidTelegramUsername(input);
    }
    public boolean isValidRequest(RequestsDraft draft) {
        return draft.getType() != null && !draft.getType().isBlank()
                && draft.getDescription() != null && !draft.getDescription().isBlank()
                && draft.getDeadline() != null
                && draft.getBudget() != null && !draft.getBudget().isBlank()
                && draft.getContact() != null && !draft.getContact().isBlank();
    }

    private void handleUserCommands(Long chatId, String text, BotState currentState) throws TelegramApiException {
        if (text.equals("📋 Меню")) {
            sendMainMenu(chatId);
            return;
        }
        switch (currentState) {
            case START -> {
                sendTextMessage(chatId, "Добро пожаловать! Введите /start, чтобы начать работу.");
            }

            case WAITING_CAPTCHA -> {
                sendTextMessage(chatId, "Пожалуйста, введите капчу для продолжения.");
            }

            case MAIN_MENU -> {
                switch (text) {
                    case "FAQ" -> sendTextMessage(chatId, "FAQ: Как я могу помочь вам?");
                    case "Создать заявку" -> {
                        requestService.startDraft(chatId);
                        botStateContextService.setBotState(chatId, BotState.ENTER_CUSTOM_PROJECT_TYPE);
                        sendProjectTypeOptions(chatId);
                    }
                    case "Мои заявки" -> {
                        sendUserRequests(chatId); // <--- новый метод
                    }
                    case "Назад" -> sendTextMessage(chatId, "Вы вернулись в главное меню.");
                }
            }

            case ENTER_CUSTOM_PROJECT_TYPE -> {
                // Если пользователь написал текст вместо выбора кнопок
                requestService.setType(chatId, text);
                botStateContextService.setBotState(chatId, BotState.ASK_PROJECT_TYPE);
            }

            case ASK_PROJECT_TYPE -> {
                requestService.setType(chatId, text);
                botStateContextService.setBotState(chatId, BotState.ASK_DESCRIPTION);
                sendTextMessage(chatId, "Опишите ваш проект:");
            }

            case ASK_DESCRIPTION -> {
                requestService.setDescription(chatId, text);
                botStateContextService.setBotState(chatId, BotState.ASK_DEADLINE);
                sendTextMessage(chatId, "Укажите сроки проекта:");
            }

            case ASK_DEADLINE -> {
                requestService.setDeadline(chatId, text);
                botStateContextService.setBotState(chatId, BotState.ASK_BUDGET);
                sendTextMessage(chatId, "Укажите бюджет:");
            }

            case ASK_BUDGET -> {
                requestService.setBudget(chatId, text);
                botStateContextService.setBotState(chatId, BotState.ASK_CONTACT);
                sendTextMessage(chatId, "Оставьте контакт для связи:");
            }

            case ASK_CONTACT -> {
                if (!isValidContact(text)) {
                    sendTextMessage(chatId, "❌ Неверный контакт. Введите номер телефона, email или Telegram username.\n\nПримеры:\n+79001234567\nuser@example.com\n@telegram_user");
                    return;
                }

                requestService.setContact(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);

                sendDraftSummaryWithActions(chatId);
            }


//            case ASK_ATTACHMENTS_DECISION -> {
//                if (text.equalsIgnoreCase("Да")) {
//                    // Переход в состояние прикрепления файлов
//                    botStateContextService.setBotState(chatId, BotState.ASK_ATTACHMENTS);
//
//                    // Информация о процессе прикрепления
//                    sendTextMessage(chatId, "Отправьте файлы одним или несколькими сообщениями.\nКогда закончите, нажмите ✅ Завершить прикрепление.");
//
//                    // Кнопка завершения процесса прикрепления
//                    sendFinishAttachmentsButton(chatId);
//                } else if (text.equalsIgnoreCase("Нет")) {
//                    // Переход к просмотру предварительной заявки
//                    botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
//                    sendDraftSummaryWithActions(chatId);
//                } else {
//                    SendMessage message = new SendMessage();
//                    message.setChatId(chatId.toString());
//                    // Если ответ некорректен, запросить еще раз
//                    sendTextMessage(chatId, "Пожалуйста, выберите один из вариантов: Да или Нет.");
//                    sendTextMessage(chatId, "Хотите прикрепить файлы к заявке? 📎");
//                    message.setReplyMarkup(yesNoInlineKeyboard("attachments_yes", "attachments_no"));
//                }
//            }

            case REVIEW_DRAFT -> {
                switch (text) {
                    case "✏️ Изменить тип проекта" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_PROJECT_TYPE);
                        sendTextMessage(chatId, "Введите новый тип проекта:");
                        sendProjectTypeOptions(chatId);
                    }
                    case "✏️ Изменить описание" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_DESCRIPTION);
                        sendTextMessage(chatId, "Введите новое описание:");
                    }
                    case "✏️ Изменить сроки" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_DEADLINE);
                        sendTextMessage(chatId, "Введите новые сроки:");
                    }
                    case "✏️ Изменить бюджет" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_BUDGET);
                        sendTextMessage(chatId, "Введите новый бюджет:");
                    }
                    case "✏️ Изменить контакт" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_CONTACT);
                        sendTextMessage(chatId, "Введите удобный способ связи: номер телефона, email или ник Telegram:");
                    }
//                    case "✅ Отправить заявку" -> {
//                        requestService.saveFinalRequest(chatId);
//                        botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
//
//                        sendTextMessage(chatId, "✅ Ваша заявка принята, ожидайте ответа.");
//                        sendMainMenu(chatId);
//                    }
                    case "✅ Отправить заявку" -> {
                        RequestsDraft draft = draftMemoryService.getDraft(chatId);

                        if (!isValidRequest(draft)) {
                            sendTextMessage(chatId, "❌ Невозможно сохранить заявку. Все поля должны быть заполнены.");
                            return;
                        }

                        requestService.saveFinalRequest(chatId);
                        botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
                        sendTextMessage(chatId, "✅ Ваша заявка принята, ожидайте ответа.");

                        sendMainMenu(chatId);
                    }


                    default -> sendTextMessage(chatId, "Пожалуйста, выберите действие с кнопок ниже.");
                }
            }

            case EDIT_PROJECT_TYPE -> {
                requestService.setType(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }

            case EDIT_DESCRIPTION -> {
                requestService.setDescription(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }

            case EDIT_DEADLINE -> {
                requestService.setDeadline(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }

            case EDIT_BUDGET -> {
                requestService.setBudget(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }

            case EDIT_CONTACT -> {
                requestService.setContact(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId);
            }
            default -> sendTextMessage(chatId, "Пожалуйста, продолжите свое действие");
        }
    }

    private void sendDraftSummaryWithActions(Long chatId) throws TelegramApiException {
        String summary = requestService.getDraftSummary(chatId);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

//        if (!draft.getAttachmentFileIds().isEmpty()) {
//            summary += "\n\n📎 Прикрепленные файлы:\n";
//
//            List<List<InlineKeyboardButton>> fileRows = new ArrayList<>();
//            int i = 1;
//            for (String fileId : draft.getAttachmentFileIds()) {
//                summary += i + ". 📄 " + fileId + "\n";
//
//                InlineKeyboardButton deleteButton = new InlineKeyboardButton("🗑 Удалить файл " + i);
//                deleteButton.setCallbackData("delete_file_" + i); // Например: delete_file_1
//                fileRows.add(List.of(deleteButton));
//                i++;
//            }
//
//            rows.addAll(fileRows);
//        } else {
//            summary += "\n\n📎 Нет прикрепленных файлов.";
//        }

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        rows.add(List.of(button("✏️ Изменить тип проекта", "edit_type"), button("✏️ Изменить описание", "edit_description")));
        rows.add(List.of(button("✏️ Изменить сроки", "edit_deadline"), button("✏️ Изменить бюджет", "edit_budget")));
        rows.add(List.of(button("✏️ Изменить контакт", "edit_contact")));
        rows.add(List.of(button("❌ Отменить заявку", "cancel_request"),button("✅ Отправить заявку", "submit_request")));

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(summary + "\n\nВы можете отредактировать, отменить или отправить заявку.");
        message.setReplyMarkup(inlineKeyboard);

        execute(message);
    }

    private void sendProjectTypeOptions(Long chatId) throws TelegramApiException {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(button("🌐 Сайт", "type:Сайт")));
        rows.add(List.of(button("📱 Мобильное приложение", "type:Мобильное приложение")));
        rows.add(List.of(button("🤖 Чат-бот", "type:Чат-бот")));
        rows.add(List.of(button("⚙️ Автоматизация", "type:Автоматизация")));
        rows.add(List.of(button("🎨 Дизайн", "type:Дизайн")));
        rows.add(List.of(button("🧠 Другое", "type:Другое")));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        sendTextMessageWithInlineKeyboard(chatId, "Выберите тип проекта или укажите свой:", markup);
    }

    private void sendMainMenu(Long chatId) throws TelegramApiException {
        SendMessage menu = new SendMessage();
        menu.setChatId(chatId.toString());
        menu.setText("Выберите действие:");

        // Создаём inline-кнопки
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("FAQ");
        button1.setCallbackData("faq");

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Мои заявки");
        button2.setCallbackData("leave_feedback");

        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Создать заявку");
        button3.setCallbackData("create_request");

        // Создаём строки с кнопками
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(button1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(button2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(button3);

        // Создаём клавиатуру
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row1, row2, row3));

        // Устанавливаем клавиатуру
        menu.setReplyMarkup(keyboard);

        // Отправляем сообщение
        execute(menu);
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private void sendTextMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);
        execute(message);
    }

    private void sendSuccessMessage(Long chatId) throws TelegramApiException {
        sendTextMessage(chatId, "✅ Проверка пройдена успешно! Теперь вы можете пользоваться ботом.");
        sendReplyKeyboardWithMenuButton(chatId);

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
            throw new TelegramApiException("Ошибка генерации капчи!", e);
        }
    }

    private void handleError(Long chatId, Exception e) {
        try {
            sendTextMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, попробуйте позже.");
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
        e.printStackTrace();
    }

}