package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.*;
import io.project.clientkeeperbot.repository.AdminResponseRepository;
import io.project.clientkeeperbot.repository.RequetRepository;
import io.project.clientkeeperbot.service.report.ChartReportService;
import io.project.clientkeeperbot.service.report.WordReportService;
import io.project.clientkeeperbot.state.BotState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RequetRepository requestRepository;
    private final BotStateContextService botStateContextService;
    private final AdminResponseRepository adminResponseRepository;
    @Autowired
    private RequestStateCacheService requestStateCacheService;
    private final WordReportService wordReportService;
    private final ChartReportService chartReportService;
    private final Map<Long, LocalDate> tempFromDateMap = new HashMap<>();
    // Обработка callback для различных кнопок
    public void handleAdminCallback(Long chatId, String callbackData, TelegramBot telegramBot) throws TelegramApiException {
        // Обработка кнопки "Показать ещё"
        if (callbackData.startsWith("more_")) {
            // Пример: more_NEW_1
            String[] parts = callbackData.split("_");
            if (parts.length == 3) {
                String statusStr = parts[1];
                int page = Integer.parseInt(parts[2]);

                try {
                    RequestStatus status = RequestStatus.valueOf(statusStr);
                    showRequests(chatId, page, status, telegramBot);
                } catch (IllegalArgumentException e) {
                    sendTextMessage(chatId, "Ошибка: неизвестный статус заявки.", telegramBot);
                }
            } else {
                sendTextMessage(chatId, "Некорректный формат команды.", telegramBot);
            }
            return; // Завершаем выполнение, чтобы не продолжать switch
        }

        if (callbackData.startsWith("reply_request_")) {
            Long requestId = Long.parseLong(callbackData.replace("reply_request_", ""));
            sendAcceptRejectOptions(chatId, requestId, telegramBot);
        }

        if (callbackData.startsWith("accept_request_")) {
            Long requestId = Long.parseLong(callbackData.replace("accept_request_", ""));
            showCommentChoice(chatId, requestId, "ACCEPT", telegramBot);
            return;
        }

        if (callbackData.startsWith("reject_request_")) {
            Long requestId = Long.parseLong(callbackData.replace("reject_request_", ""));
            showCommentChoice(chatId, requestId, "REJECT", telegramBot);
            return;
        }

        if (callbackData.startsWith("comment_yes_")) {
            String[] parts = callbackData.split("_");
            String status = parts[2]; // ACCEPT или REJECT
            Long requestId = Long.parseLong(parts[3]);
            if (status.equals("ACCEPT")) {
                botStateContextService.setBotState(chatId, BotState.AWAITING_COMMENT);
                saveRequestIdForState(chatId, requestId);
                requestStateCacheService.saveModerationStatus(chatId, RequestStatus.ACCEPTED);
                sendTextMessage(chatId, "Введите комментарий для принятия заявки #" + requestId + ":", telegramBot);
            } else if (status.equals("REJECT")) {
                botStateContextService.setBotState(chatId, BotState.AWAITING_COMMENT);
                saveRequestIdForState(chatId, requestId);
                requestStateCacheService.saveModerationStatus(chatId, RequestStatus.REJECTED);
                sendTextMessage(chatId, "Введите комментарий для отклонения заявки #" + requestId + ":", telegramBot);
            }
            return;
        }
        if (callbackData.startsWith("comment_no_")) {
            String[] parts = callbackData.split("_");
            String status = parts[2];
            Long requestId = Long.parseLong(parts[3]);
            RequestStatus finalStatus = status.equals("ACCEPT") ? RequestStatus.ACCEPTED : RequestStatus.REJECTED;
            completeModeration(chatId, requestId, finalStatus, "", telegramBot);
            return;
        }

        BotStateContext context = botStateContextService.getBotStateContext(chatId);
        if (context != null && context.getCurrentState() == BotState.CHART_PERIOD_SELECTION) {
            if (callbackData.equals("chart_7")) {
                generateChartForPeriod(chatId, LocalDate.now().minusDays(6), LocalDate.now(), telegramBot);
            } else if (callbackData.equals("chart_30")) {
                generateChartForPeriod(chatId, LocalDate.now().minusDays(29), LocalDate.now(), telegramBot);
            } else if (callbackData.equals("chart_custom")) {
                botStateContextService.setBotState(chatId, BotState.CHART_CUSTOM_DATE_FROM);
                sendTextMessage(chatId, "Введите начальную дату (формат YYYY-MM-DD):", telegramBot);
            }
        }
        switch (callbackData) {
            case "moderation":
                sendTextMessage(chatId, "Вы выбрали модерацию заявок.", telegramBot);
                showModerationOptions(chatId, telegramBot);
                break;
            case "moderation_new":
                showRequests(chatId, 0, RequestStatus.NEW, telegramBot);
                break;
            case "moderation_changed":
                showRequests(chatId, 0, RequestStatus.CHANGED, telegramBot);
                break;
            case "addfaq":
                sendTextMessage(chatId, "Вы выбрали добавление FAQ.", telegramBot);
                break;
            case "report":
                showReportOptions(chatId, telegramBot);
                break;

            case "report_chart":
                botStateContextService.setBotState(chatId, BotState.CHART_PERIOD_SELECTION);
                sendChartPeriodOptions(chatId, telegramBot);
                break;

            case "report_2":
                botStateContextService.setBotState(chatId, BotState.CHART_PERIOD_SELECTION);
                sendAdminActivityPeriodOptions(chatId, telegramBot);
                break;

            case "chart_admin_7":
                generateAdminActivityChartForPeriod(chatId, LocalDate.now().minusDays(6), LocalDate.now(), telegramBot);
                break;
            case "chart_admin_30":
                generateAdminActivityChartForPeriod(chatId, LocalDate.now().minusDays(29), LocalDate.now(), telegramBot);
                break;
            case "chart_admin_custom":
                // Здесь ты можешь переключить бота в состояние выбора периода вручную
                botStateContextService.setBotState(chatId, BotState.CHART_CUSTOM_DATE_FROM_ADMIN);
                sendTextMessage(chatId, "Введите начальную дату (формат YYYY-MM-DD):", telegramBot);
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
    public void handleAdminText(Long chatId, String messageText, TelegramBot telegramBot) {
        BotStateContext context = botStateContextService.getBotStateContext(chatId);

        if (messageText.equals("📋 Админ-панель")) {
            try {
                showAdminPanel(chatId, telegramBot);
            } catch (TelegramApiException e) {
                e.printStackTrace(); // Или логгируй через логгер
            }
            return;
        }
        if (context != null && context.getCurrentState() == BotState.CHART_CUSTOM_DATE_FROM) {
            try {
                LocalDate from = LocalDate.parse(messageText.trim());
                tempFromDateMap.put(chatId, from);
                botStateContextService.setBotState(chatId, BotState.CHART_CUSTOM_DATE_TO);
                sendTextMessage(chatId, "Введите конечную дату (формат YYYY-MM-DD):", telegramBot);
            } catch (DateTimeParseException e) {
                sendTextMessage(chatId, "Неверный формат даты. Попробуйте снова: YYYY-MM-DD", telegramBot);
            }
            return;
        }
        if (context != null && context.getCurrentState() == BotState.CHART_CUSTOM_DATE_FROM_ADMIN) {
            try {
                LocalDate from = LocalDate.parse(messageText.trim());
                tempFromDateMap.put(chatId, from);
                botStateContextService.setBotState(chatId, BotState.CHART_CUSTOM_DATE_TO_ADMIN);
                sendTextMessage(chatId, "Введите конечную дату (формат YYYY-MM-DD):", telegramBot);
            } catch (DateTimeParseException e) {
                sendTextMessage(chatId, "Неверный формат даты. Попробуйте снова: YYYY-MM-DD", telegramBot);
            }
            return;
        }
        if (context != null && context.getCurrentState() == BotState.ENTER_CHART_PERIOD) {
            try {
                String[] parts = messageText.split("\\s+");
                LocalDate from = LocalDate.parse(parts[0]);
                LocalDate to = LocalDate.parse(parts[1]);

                generateChartForPeriod(chatId, from, to, telegramBot);
                botStateContextService.setBotState(chatId, BotState.DEFAULT);
            } catch (Exception e) {
                sendTextMessage(chatId, "Неверный формат. Введите в виде `yyyy-MM-dd yyyy-MM-dd`", telegramBot);
            }
        }

        if (context != null && context.getCurrentState() == BotState.ENTER_CHART_PERIOD_ADMIN) {
            try {
                String[] parts = messageText.split("\\s+");
                LocalDate from = LocalDate.parse(parts[0]);
                LocalDate to = LocalDate.parse(parts[1]);

                generateAdminActivityChartForPeriod(chatId, from, to, telegramBot);
                botStateContextService.setBotState(chatId, BotState.DEFAULT);
            } catch (Exception e) {
                sendTextMessage(chatId, "Неверный формат. Введите в виде `yyyy-MM-dd yyyy-MM-dd`", telegramBot);
            }
        }

        if (context != null && context.getCurrentState() == BotState.CHART_CUSTOM_DATE_TO) {
            try {
                LocalDate to = LocalDate.parse(messageText.trim());
                LocalDate from = tempFromDateMap.get(chatId);
                generateChartForPeriod(chatId, from, to, telegramBot);
                tempFromDateMap.remove(chatId);
            } catch (DateTimeParseException e) {
                sendTextMessage(chatId, "Неверный формат даты. Попробуйте снова: YYYY-MM-DD", telegramBot);
            }
            return;
        }

        if (context != null && context.getCurrentState() == BotState.CHART_CUSTOM_DATE_TO_ADMIN) {
            try {
                LocalDate to = LocalDate.parse(messageText.trim());
                LocalDate from = tempFromDateMap.get(chatId);
                generateAdminActivityChartForPeriod(chatId, from, to, telegramBot);
                tempFromDateMap.remove(chatId);
            } catch (DateTimeParseException e) {
                sendTextMessage(chatId, "Неверный формат даты. Попробуйте снова: YYYY-MM-DD", telegramBot);
            }
            return;
        }
        if (messageText.equals("/report_chart")) {
            botStateContextService.setBotState(chatId, BotState.CHART_PERIOD_SELECTION);
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder().text("Последние 7 дней").callbackData("chart_7").build(),
                            InlineKeyboardButton.builder().text("Последний месяц").callbackData("chart_30").build()
                    ))
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder().text("Указать вручную").callbackData("chart_custom").build()
                    ))
                    .build();
            try {
                telegramBot.execute(SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Выберите период для построения графика:")
                        .replyMarkup(markup)
                        .build());
            } catch (TelegramApiException e) {
                e.printStackTrace(); // или логгировать через логгер
            }

        }
        // Обработка ввода комментария к заявке
        if (context != null && context.getCurrentState() == BotState.AWAITING_COMMENT) {
            Long requestId = requestStateCacheService.getRequestId(chatId);
            RequestStatus requestStatus = requestStateCacheService.getModerationStatus(chatId);
            if (requestId != null && requestStatus != null) {
                try {
                    completeModeration(chatId, requestId, requestStatus, messageText, telegramBot);
                    sendTextMessage(chatId, "Комментарий сохранён и заявка обновлена.", telegramBot);
                } catch (TelegramApiException e) {
                        sendTextMessage(chatId, "Ошибка при отправке сообщения Telegram.", telegramBot);
                    }
            } else {
                    sendTextMessage(chatId, "Ошибка: заявка или статус не найдены.", telegramBot);
                }

            botStateContextService.setBotState(chatId, BotState.DEFAULT);
            requestStateCacheService.clearRequestId(chatId);
            requestStateCacheService.clearModerationStatus(chatId);
        }
    }
    private void showReportOptions(Long chatId, TelegramBot bot) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("📈 График заявок").callbackData("report_chart").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("📄 График активности менеджеров").callbackData("report_2").build(),
                        InlineKeyboardButton.builder().text("📄 Отчёт 3").callbackData("report_3").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("back_to_admin_panel").build()
                ))
                .build();

        sendTextMessage(chatId, "Выберите тип отчета:", bot, markup);
    }

    private void sendAdminActivityPeriodOptions(Long chatId, TelegramBot bot) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("Последние 7 дней").callbackData("chart_admin_7").build(),
                        InlineKeyboardButton.builder().text("Последний месяц").callbackData("chart_admin_30").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("Указать вручную").callbackData("chart_admin_custom").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("report").build()
                ))
                .build();

        sendTextMessage(chatId, "Выберите период для построения графика активности менеджеров:", bot, markup);
    }
    private void generateAdminActivityChartForPeriod(Long chatId, LocalDate from, LocalDate to, TelegramBot bot) {
        try {
            RequestCountChart chart = chartReportService.generateAndSaveChartAdmin(from, to, chatId); // возвращает путь к файлу
            File file = new File(chart.getFilePath());

            bot.execute(SendDocument.builder()
                    .chatId(chatId.toString())
                    .document(new InputFile(file))
                    .caption("График активности менеджеров с " + from + " по " + to)
                    .build());

            botStateContextService.setBotState(chatId, BotState.DEFAULT);
        } catch (Exception e) {
            sendTextMessage(chatId, "Ошибка при генерации графика активности: " + e.getMessage(), bot);
        }
    }


    private void generateChartForPeriod(Long chatId, LocalDate from, LocalDate to, TelegramBot bot) {
        try {
            RequestCountChart chart = chartReportService.generateAndSaveChart(from, to, chatId);
            File file = new File(chart.getFilePath());

            bot.execute(SendDocument.builder()
                    .chatId(chatId.toString())
                    .document(new InputFile(file))
                    .caption("График количества заявок с " + from + " по " + to)
                    .build());

            botStateContextService.setBotState(chatId, BotState.DEFAULT);
        } catch (Exception e) {
            sendTextMessage(chatId, "Ошибка при генерации графика: " + e.getMessage(), bot);
        }
    }

    private void sendChartPeriodOptions(Long chatId, TelegramBot bot) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("Последние 7 дней").callbackData("chart_7").build(),
                        InlineKeyboardButton.builder().text("Последний месяц").callbackData("chart_30").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("Указать вручную").callbackData("chart_custom").build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("⬅️ Назад").callbackData("report").build()
                ))
                .build();

        sendTextMessage(chatId, "Выберите период для построения графика:", bot, markup);
    }

    private void saveRequestIdForState(Long chatId, Long requestId) {
        requestStateCacheService.saveRequestId(chatId, requestId);
    }

    public void completeModeration(Long chatId, Long requestId, RequestStatus status, String comment, TelegramBot telegramBot) throws TelegramApiException {
    Optional<Request> optionalRequest = requestRepository.findById(requestId);
    if (optionalRequest.isEmpty()) {
        sendTextMessage(chatId, "Заявка с ID " + requestId + " не найдена.", telegramBot);
        return;
    }

    Request request = optionalRequest.get();

    // Обновляем статус заявки
    request.setStatus(status);
    requestRepository.save(request);

    // Сохраняем ответ администратора
    AdminResponse response = new AdminResponse();
    response.setRequest(request);
    response.setAdminId(chatId);
    response.setResponseText(comment);
    response.setResponseDate(LocalDateTime.now());
    response.setStatus(status);
    adminResponseRepository.save(response);


        // Генерация и сохранение Word-отчета
        try {
            WordReport report = wordReportService.generateAndSaveReport(request, response, chatId);
            File file = new File(report.getFilePath());

            if (file.exists()) {
                SendDocument sendDoc = new SendDocument();
                sendDoc.setChatId(chatId);
                sendDoc.setDocument(new InputFile(file));
                telegramBot.execute(sendDoc);
            } else {
                sendTextMessage(chatId, "⚠️ Не удалось найти файл отчета для отправки.", telegramBot);
            }
        } catch (Exception e) {
            sendTextMessage(chatId, "⚠️ Произошла ошибка при генерации Word-отчета: " + e.getMessage(), telegramBot);
            e.printStackTrace();
        }
    // Уведомляем пользователя
    Long clientId = request.getClientId();
    String statusEmoji = status == RequestStatus.ACCEPTED ? "✅" : "❌";

    StringBuilder userMessage = new StringBuilder();
    userMessage.append(statusEmoji)
            .append(" Ваша заявка №").append(requestId)
            .append(" была ").append(status == RequestStatus.ACCEPTED ? "принята." : "отклонена.");

    if (comment != null && !comment.isBlank()) {
        userMessage.append("\n\nКомментарий администратора:\n").append(comment);
    }

    telegramBot.execute(SendMessage.builder()
            .chatId(clientId.toString())
            .text(userMessage.toString())
            .build());

    // Подтверждение админу
    sendTextMessage(chatId, "Заявка #" + requestId + " успешно модерирована и пользователь уведомлён.", telegramBot);
}

    private void showCommentChoice(Long chatId, Long requestId, String status, TelegramBot telegramBot) throws TelegramApiException {
        InlineKeyboardButton yes = new InlineKeyboardButton("💬 Да");
        yes.setCallbackData("comment_yes_" + status + "_" + requestId);

        InlineKeyboardButton no = new InlineKeyboardButton("⛔ Нет");
        no.setCallbackData("comment_no_" + status + "_" + requestId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(yes, no)));

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Хотите добавить комментарий к ответу?");
        message.setReplyMarkup(markup);

        telegramBot.execute(message);
    }

    public void sendAcceptRejectOptions(Long chatId, Long requestId, TelegramBot bot) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Выберите действие для заявки #" + requestId);

        InlineKeyboardButton accept = new InlineKeyboardButton("✅ Принять");
        accept.setCallbackData("accept_request_" + requestId);

        InlineKeyboardButton reject = new InlineKeyboardButton("❌ Отклонить");
        reject.setCallbackData("reject_request_" + requestId);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(accept, reject)));

        msg.setReplyMarkup(markup);
        bot.execute(msg);
    }

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

    public void showModerationOptions(Long chatId, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите тип заявок:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton newRequests = new InlineKeyboardButton();
        newRequests.setText("📥 Новые заявки");
        newRequests.setCallbackData("moderation_new");

        InlineKeyboardButton changedRequests = new InlineKeyboardButton();
        changedRequests.setText("📝 Измененные заявки");
        changedRequests.setCallbackData("moderation_changed");

        rows.add(List.of(newRequests));
        rows.add(List.of(changedRequests));
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);
        telegramBot.execute(message);
    }

    public void showRequests(Long chatId, int page, RequestStatus status, TelegramBot bot) throws TelegramApiException {
        int pageSize = 5;
        List<Request> requests = requestRepository.findByStatus(status, PageRequest.of(page, pageSize)).getContent();

        if (requests.isEmpty()) {
            sendTextMessage(chatId, "Нет заявок с этим статусом.", bot);
            return;
        }

        for (Request req : requests) {
            String messageText = String.format(
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

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText(messageText);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton replyButton = new InlineKeyboardButton("Ответить");
            replyButton.setCallbackData("reply_request_" + req.getId());

            markup.setKeyboard(List.of(List.of(replyButton)));
            msg.setReplyMarkup(markup);

            bot.execute(msg);
        }

        // Кнопка "Показать ещё"
        SendMessage more = new SendMessage();
        more.setChatId(chatId.toString());
        more.setText("Показать ещё?");

        InlineKeyboardButton moreBtn = new InlineKeyboardButton("➡️ Ещё");
        moreBtn.setCallbackData("more_" + status.name() + "_" + (page + 1));
        InlineKeyboardMarkup moreMarkup = new InlineKeyboardMarkup();
        moreMarkup.setKeyboard(List.of(List.of(moreBtn)));

        more.setReplyMarkup(moreMarkup);
        bot.execute(more);
    }

    public void sendReplyKeyboardWithMenuButtonAdmin(Long chatId, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Обратитесь в Админ-панель"); // Отправит пробел — Telegram примет

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("📋 Админ-панель"));
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);

        markup.setOneTimeKeyboard(false); // чтобы кнопка не исчезала

        message.setReplyMarkup(markup);

        telegramBot.execute(message);
    }

    // Метод для отправки текстового сообщения
    public void sendTextMessage(Long chatId, String text, TelegramBot telegramBot) {
        try {
            telegramBot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace(); // или логгировать
        }
    }

    private void sendTextMessage(Long chatId, String text, TelegramBot bot, InlineKeyboardMarkup markup) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(markup)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace(); // логгируй, если хочешь
        }
    }
    // Метод для отображения кнопок для управления администраторами
    public void showSystemOptions(Long chatId, TelegramBot telegramBot) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Система: Выберите действие:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

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
}