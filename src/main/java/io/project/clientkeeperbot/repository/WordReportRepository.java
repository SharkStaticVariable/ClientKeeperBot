package io.project.clientkeeperbot.repository;

import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.WordReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordReportRepository extends JpaRepository<WordReport, Long> {
    Optional<WordReport> findByRequest(Request request);
}
