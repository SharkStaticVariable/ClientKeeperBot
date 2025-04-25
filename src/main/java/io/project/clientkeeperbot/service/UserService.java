package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.Users;
import io.project.clientkeeperbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserService {

private final UserRepository userRepo;

    public void registerUser(User telegramUser) {
        if (!userRepo.existsByTelegramId(telegramUser.getId())) {
            Users user = new Users();
            user.setTelegramId(telegramUser.getId());
            user.setUserName(telegramUser.getUserName());
            user.setFirstName(telegramUser.getFirstName());
            user.setLastName(telegramUser.getLastName());
            userRepo.save(user);
        }
    }

}