package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @NotNull
    private String type;
    private String description;
    private String deadline;
    @NotNull
    private String budget;
    private String contact;

//    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Attachment> attachments = new ArrayList<>();


    private LocalDateTime createdAt = LocalDateTime.now();
}
