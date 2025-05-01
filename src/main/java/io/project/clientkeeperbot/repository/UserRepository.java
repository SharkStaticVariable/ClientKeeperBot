package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByTelegramId(Long telegramId);
//    boolean existsByChatId(Long chatId);

    Optional<Users> findByTelegramId(Long telegramId);
    boolean existsByUserName(String userName);
    // Найти пользователя по Telegram ID
//    Optional<Users> findByTelegramId(Long telegramId);

    // Проверить существование пользователя по username
//    boolean existsByUsername(String userName);

    // Поиск по имени (с пагинацией)
//    Page<Users> findByFullNameContainingIgnoreCase(String name, Pageable pageable);



    // Удалить по Telegram ID (возвращает количество удаленных записей)
//    @Modifying
//    @Query("DELETE FROM User u WHERE u.telegramId = :telegramId")
//    int deleteByTelegramId(@Param("telegramId") Long telegramId);
}