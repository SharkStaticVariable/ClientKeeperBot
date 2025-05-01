package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.RequestsDraft;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestDraftMemoryService {
    private final Map<Long, RequestsDraft> draftMap = new ConcurrentHashMap<>();

    public RequestsDraft getDraft(Long userId) {
        return draftMap.computeIfAbsent(userId, id -> new RequestsDraft());
    }

    public void clearDraft(Long userId) {
        draftMap.remove(userId);
    }


//    public void addAttachment(Long chatId, String fileId) {
//        RequestsDraft draft = getDraft(chatId);
//        if (draft.getAttachmentFileIds().size() >= 3) {
//            throw new IllegalStateException("Нельзя прикрепить больше 3 файлов.");
//        }
//        draft.getAttachmentFileIds().add(fileId);
//    }
//
//    public void removeAttachment(Long chatId, int index) {
//        RequestsDraft draft = getDraft(chatId);
//        if (index >= 0 && index < draft.getAttachmentFileIds().size()) {
//            draft.getAttachmentFileIds().remove(index);
//        }
//    }


}
