//package io.project.clientkeeperbot.service;
//
//
//import io.project.clientkeeperbot.entity.Attachment;
//import io.project.clientkeeperbot.entity.Request;
//import io.project.clientkeeperbot.repository.AttachmentRepository;
//import io.project.clientkeeperbot.repository.RequetRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class AttachmentService {
//
//    private final AttachmentRepository attachmentRepository;
//    private final RequetRepository requestRepository;
//
//    @Transactional
//    public void addAttachmentToRequest(Long requestId, String fileId) {
//        Request request = requestRepository.findById(requestId)
//                .orElseThrow(() -> new IllegalArgumentException("Request not found with id: " + requestId));
//
//        Attachment attachment = new Attachment();
//        attachment.setFileId(fileId);
//        attachment.setRequest(request);
//
//        request.getAttachments().add(attachment);
//        requestRepository.save(request); // благодаря cascade вложение само сохранится
//    }
//
//    @Transactional
//    public void removeAttachment(Long attachmentId) {
//        attachmentRepository.deleteById(attachmentId);
//    }
//}
