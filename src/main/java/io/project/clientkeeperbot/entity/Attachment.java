//package io.project.clientkeeperbot.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Data
//@NoArgsConstructor
//public class Attachment {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id; // вот твой ID для каждого файла
//
//    private String fileId; // file_id из Telegram
//
//    @ManyToOne
//    @JoinColumn(name = "request_id")
//    private Request request;
//}