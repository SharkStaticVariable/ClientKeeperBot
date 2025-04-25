package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.state.BotState;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CommandAccessFilter {
    // Разрешённые команды в любом состоянии
    private static final Set<String> UNIVERSAL_COMMANDS = Set.of("❌ Отменить создание заявки");

    // Разрешённые команды только в MAIN_MENU
    private static final Set<String> MAIN_MENU_COMMANDS = Set.of("FAQ", "Мои заявки", "Создать заявку");

    public boolean isAllowed(BotState state, String command) {
        if (UNIVERSAL_COMMANDS.contains(command)) return true;

        if (state == BotState.MAIN_MENU || state == BotState.WAITING_CAPTCHA) {
            return MAIN_MENU_COMMANDS.contains(command);
        }

        return false;
    }
}
