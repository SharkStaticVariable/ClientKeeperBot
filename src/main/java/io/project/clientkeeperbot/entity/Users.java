package io.project.clientkeeperbot.entity;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "telegram_id", unique = true)
    private Long telegramId; // ID пользователя в Telegram
    private String firstName;
    private String lastName;
    @Column(name = "username")
    private String userName;
    // другие необходимые поля
}