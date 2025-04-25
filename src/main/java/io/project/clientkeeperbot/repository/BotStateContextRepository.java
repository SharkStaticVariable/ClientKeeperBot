package io.project.clientkeeperbot.repository;
import io.project.clientkeeperbot.entity.BotStateContext;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotStateContextRepository extends JpaRepository<BotStateContext, String> {
}
