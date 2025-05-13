package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequetRepository extends JpaRepository<Request, Long> {
    List<Request> findByClientId(Long clientId);

    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    List<Request> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

}
