package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.Admins;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admins, Long> {

    // Поиск администратора по tgId
    Optional<Admins> findByTgId(Long tgId);
}
