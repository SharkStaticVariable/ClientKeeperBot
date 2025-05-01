package io.project.clientkeeperbot.state;

public enum BotState {
    // Состояния для пользователей
    START,
    WAITING_CAPTCHA,
    MAIN_MENU,

    // Состояния для администраторов
    ADMIN_MAIN_MENU,
    MODERATE_TICKETS,
    ADD_FAQ,
    REPORTS_MENU,
    SYSTEM_SETTINGS,


    READY,
    ENTER_CUSTOM_PROJECT_TYPE,
    CONFIRM_CANCEL,
    ATTACHMENT_WAITING_FILE, // ожидание, что пользователь отправит файл
    ASK_ATTACHMENTS_DECISION,
    ASK_ATTACHMENTS,
    WAITING_FOR_ATTACHMENTS,

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

}