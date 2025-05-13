package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.AdminResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminResponseRepository extends JpaRepository<AdminResponse, Long> {
    List<AdminResponse> findAllByResponseDateBetween(LocalDateTime start, LocalDateTime end);

}
