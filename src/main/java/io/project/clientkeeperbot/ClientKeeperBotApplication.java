package io.project.clientkeeperbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
        "io.project.clientkeeperbot.entity",
        "org.springframework.statemachine.data.jpa"
})
@EnableJpaRepositories(basePackages = "io.project.clientkeeperbot.repository")
public class ClientKeeperBotApplication {

    public static void main(String[] args) {

        SpringApplication.run(ClientKeeperBotApplication.class, args);
    }

}
