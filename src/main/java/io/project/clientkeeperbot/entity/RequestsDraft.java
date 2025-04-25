package io.project.clientkeeperbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
public class RequestsDraft {
    private Long clientId;

    private String type;
    private String description;
    private String deadline;
    private String budget;
    private String contact;

    @ElementCollection
    private List<String> attachmentPaths = new ArrayList<>();

}
