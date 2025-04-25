package io.project.clientkeeperbot.state;

public enum BotState {
    // Состояния для пользователей
    START,
    WAITING_CAPTCHA,
    MAIN_MENU,
    CREATING_TICKET,
    TICKET_DETAILS_INPUT,

    // Состояния для администраторов
    ADMIN_MAIN_MENU,
    MODERATE_TICKETS,
    ADD_FAQ,
    REPORTS_MENU,
    SYSTEM_SETTINGS,

    PROJECT_TYPE,
    PROJECT_DESCRIPTION,
    PROJECT_DEADLINE,
    PROJECT_BUDGET,
    PROJECT_CONTACT,
    PROJECT_ATTACHMENTS,
    PROJECT_COMPLETE,
    READY,

    REVIEW_REQUEST,                    // Просмотр и редактирование заявки
    EDIT_FIELD_SELECTION,
    EDITING_FIELD,
    EDIT_PROJECT_TYPE,
    EDIT_DESCRIPTION,
    EDIT_DEADLINE,
    EDIT_BUDGET,
    EDIT_CONTACT,

    ASK_PROJECT_TYPE,
    ASK_DESCRIPTION,
    ASK_DEADLINE,
    ASK_BUDGET,
    ASK_CONTACT,
    REVIEW_DRAFT,

    AWAITING_PROJECT_TYPE,        // Ожидает тип проекта
    AWAITING_PROJECT_DESCRIPTION, // Ожидает краткое описание
    AWAITING_PROJECT_DEADLINE,    // Ожидает срок
    AWAITING_PROJECT_BUDGET,      // Ожидает бюджет
    AWAITING_PROJECT_CONTACT,     // Ожидает контакт
    AWAITING_PROJECT_FILES,       // Ожидает файлы
    FINAL_SUBMISSION,             // Завершение и отправка заявки
    IDLE                          // Ожидание команды

}