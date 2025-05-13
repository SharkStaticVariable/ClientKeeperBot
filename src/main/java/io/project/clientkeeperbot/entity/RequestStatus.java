package io.project.clientkeeperbot.entity;

public enum RequestStatus {
    NEW("Новая"),
    CHANGED("Изменена"),
    ACCEPTED("Принята"),
    REJECTED("Отклонена");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
