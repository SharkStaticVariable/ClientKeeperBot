package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.config.BotConfig;
import io.project.clientkeeperbot.entity.BotStateContext;
import io.project.clientkeeperbot.entity.Request;
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
    private final CommandAccessFilter commandAccessFilter;

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
        // Обработка сообщений с текстом
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            try {
                BotStateContext botStateContext = botStateContextService.getBotStateContext(chatId);

                if (text.equals("/start")) {
                    handleStartCommand(chatId, update);
                    return;
                }

                if (botStateContext == null) {
                    botStateContextService.setBotState(chatId, BotState.START);
                    return;
                }

                BotState currentState = botStateContext.getCurrentState();

                if (currentState == BotState.WAITING_CAPTCHA) {
                    handleCaptchaInput(chatId, text, update.getMessage().getFrom(), botStateContext);
                    return;
                }

                if (adminAuthService.isAdmin(chatId)) {
                    handleAdminCommands(chatId, text);
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

            try {
                // Передаем весь объект CallbackQuery в handleCallbackQuery
                handleCallbackQuery(callbackQuery);
            } catch (Exception e) {
                Long chatId = callbackQuery.getMessage().getChatId(); // получаем chatId из callbackQuery в случае ошибки
                handleError(chatId, e);
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();

        switch (callbackData) {
            case "faq":
                sendTextMessage(chatId, "FAQ: Как я могу помочь вам?");
                break;
            case "leave_feedback":
                sendTextMessage(chatId, "ваши заявки.");
                sendUserRequests(chatId);
                break;
            case "create_request":
                requestService.startDraft(chatId);
                botStateContextService.setBotState(chatId, BotState.ASK_PROJECT_TYPE);
                sendTextMessage(chatId, "Выберите тип проекта:");

                break;
            default:
                sendTextMessage(chatId, "Неизвестная команда.");
                break;
        }
        // В зависимости от callbackData, выполняем действия
        switch (callbackData) {
            case "edit_type":
                botStateContextService.setBotState(chatId, BotState.EDIT_PROJECT_TYPE);
                sendTextMessage(chatId, "Введите новый тип проекта:");
                break;
            case "edit_description":
                botStateContextService.setBotState(chatId, BotState.EDIT_DESCRIPTION);
                sendTextMessage(chatId, "Введите новое описание:");
                break;
            case "edit_deadline":
                botStateContextService.setBotState(chatId, BotState.EDIT_DEADLINE);
                sendTextMessage(chatId, "Введите новые сроки:");
                break;
            case "edit_budget":
                botStateContextService.setBotState(chatId, BotState.EDIT_BUDGET);
                sendTextMessage(chatId, "Введите новый бюджет:");
                break;
            case "edit_contact":
                botStateContextService.setBotState(chatId, BotState.EDIT_CONTACT);
                sendTextMessage(chatId, "Введите новый контакт:");
                break;
            case "submit_request":
                requestService.saveFinalRequest(chatId);
                botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
                sendTextMessage(chatId, "✅ Ваша заявка принята, ожидайте ответа.");
                sendMainMenu(chatId);
                break;
            default:
                sendTextMessage(chatId, "Неизвестная команда.");
        }
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


    private void handleCaptchaInput(Long chatId, String inputText, User telegramUser, BotStateContext botStateContext) throws TelegramApiException {
        if (captchaService.verifyCaptcha(chatId, inputText)) {
            userService.registerUser(telegramUser);
            sendSuccessMessage(chatId);
            sendMainMenu(chatId);
            botStateContextService.setBotState(chatId, BotState.MAIN_MENU);
        } else {
            sendTextMessage(chatId, "Неверный код капчи. Попробуйте еще раз.");
            sendCaptcha(chatId);
        }
    }

    private void handleAdminCommands(Long chatId, String text) throws TelegramApiException {
        if (text.equals("/admin")) {
            adminService.showAdminPanel(chatId, this);
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
                    req.getStatus()
            );

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton editButton = new InlineKeyboardButton("✏️ Редактировать");
            editButton.setCallbackData("edit_request:" + req.getId());

            rows.add(List.of(editButton));
            markup.setKeyboard(rows);

            try {
                sendTextMessageWithInlineKeyboard(chatId, message, markup);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendTextMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);
        execute(message);
    }



    private void handleUserCommands(Long chatId, String text, BotState currentState) throws TelegramApiException {
        if (!commandAccessFilter.isAllowed(currentState, text)) {
            sendTextMessage(chatId, "Вы сейчас в процессе создания заявки. Завершите текущий шаг или нажмите ❌ Отменить создание заявки.");
            return;
        }
        switch (currentState) {

            case MAIN_MENU -> {
                switch (text) {
                    case "FAQ" -> sendTextMessage(chatId, "FAQ: Как я могу помочь вам?");
                    case "Создать заявку" -> {
                        requestService.startDraft(chatId);
                        botStateContextService.setBotState(chatId, BotState.ASK_PROJECT_TYPE);
                        sendTextMessage(chatId, "Выберите тип проекта:");
                        sendProjectTypeOptions(chatId);
                    }
                    case "Мои заявки" -> {
                        sendUserRequests(chatId); // <--- новый метод
                    }

                    case "Назад" -> sendTextMessage(chatId, "Вы вернулись в главное меню.");
                }
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
                requestService.setContact(chatId, text);
                botStateContextService.setBotState(chatId, BotState.REVIEW_DRAFT);
                sendDraftSummaryWithActions(chatId); // Показываем заявку с кнопками редактирования/отправки
            }

            case REVIEW_DRAFT -> {
                switch (text) {
                    case "✏️ Изменить тип проекта" -> {
                        botStateContextService.setBotState(chatId, BotState.EDIT_PROJECT_TYPE);
                        sendTextMessage(chatId, "Введите новый тип проекта:");
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
                        sendTextMessage(chatId, "Введите новый контакт:");
                    }
                    case "✅ Отправить заявку" -> {
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
        }
    }
    private void sendReviewButtons(Long chatId) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Вы можете изменить заявку или отправить её:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(List.of(
                new KeyboardRow(List.of(new KeyboardButton("Изменить заявку"))),
                new KeyboardRow(List.of(new KeyboardButton("Отправить")))
        ));
        keyboard.setResizeKeyboard(true);
        msg.setReplyMarkup(keyboard);
        execute(msg);
    }

    private void sendEditFieldButtons(Long chatId) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Что хотите изменить?");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(List.of(
                new KeyboardRow(List.of(new KeyboardButton("Тип проекта"))),
                new KeyboardRow(List.of(new KeyboardButton("Описание"))),
                new KeyboardRow(List.of(new KeyboardButton("Сроки"))),
                new KeyboardRow(List.of(new KeyboardButton("Бюджет"))),
                new KeyboardRow(List.of(new KeyboardButton("Контакт")))
        ));
        keyboard.setResizeKeyboard(true);
        msg.setReplyMarkup(keyboard);
        execute(msg);
    }

    private void sendDraftSummaryWithActions(Long chatId) throws TelegramApiException {
        String summary = requestService.getDraftSummary(chatId);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(button("✏️ Изменить тип проекта", "edit_type"), button("✏️ Изменить описание", "edit_description")));
        rows.add(List.of(button("✏️ Изменить сроки", "edit_deadline"), button("✏️ Изменить бюджет", "edit_budget")));
        rows.add(List.of(button("✏️ Изменить контакт", "edit_contact")));
        rows.add(List.of(button("✅ Отправить заявку", "submit_request")));

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(summary + "\n\nВы можете отредактировать поля или отправить заявку.");
        message.setReplyMarkup(inlineKeyboard);

        execute(message);
    }

    private void sendProjectTypeOptions(Long chatId) throws TelegramApiException {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createButton("🌐 Сайт", "type:Сайт")));
        rows.add(List.of(createButton("📱 Мобильное приложение", "type:Мобильное приложение")));
        rows.add(List.of(createButton("🤖 Телеграм-бот", "type:Телеграм-бот")));
        rows.add(List.of(createButton("⚙️ Автоматизация", "type:Автоматизация")));
        rows.add(List.of(createButton("🎨 Дизайн", "type:Дизайн")));
        rows.add(List.of(createButton("🧠 Другое", "type:Другое")));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        sendTextMessageWithInlineKeyboard(chatId, "Выберите тип проекта:", markup);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }


    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }




    private void sendSuccessMessage(Long chatId) throws TelegramApiException {
        sendTextMessage(chatId, "✅ Проверка пройдена успешно! Теперь вы можете пользоваться ботом.");
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

    private void handleError(Long chatId, Exception e) {
        try {
            sendTextMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, попробуйте позже.");
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
        e.printStackTrace();
    }
}
