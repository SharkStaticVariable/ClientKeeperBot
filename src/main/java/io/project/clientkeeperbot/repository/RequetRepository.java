package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequetRepository extends JpaRepository<Request, Long> {
    List<Request> findByClientId(Long clientId);

}
