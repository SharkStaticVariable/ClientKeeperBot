package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.Admins;
import io.project.clientkeeperbot.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;

    // Проверка, является ли пользователь администратором
    public boolean isAdmin(Long tgId) {
        return adminRepository.findByTgId(tgId).isPresent();
    }

    // Добавление администратора
    public void addAdmin(Long tgId) {
        Admins admin = new Admins();
        admin.setTgId(tgId);
        adminRepository.save(admin);
    }

    // Удаление администратора
    public void removeAdmin(Long tgId) {
        adminRepository.deleteById(tgId);
    }
}
//    private static final List<Long> ADMIN_IDS = List.of(6180241984L); // Список ID админов
