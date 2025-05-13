package io.project.clientkeeperbot.service;

//import io.project.clientkeeperbot.entity.Attachment;
import io.project.clientkeeperbot.entity.Request;
import io.project.clientkeeperbot.entity.RequestStatus;
import io.project.clientkeeperbot.entity.RequestsDraft;
import io.project.clientkeeperbot.repository.RequetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RequestService {
    private final RequetRepository requestRepository;
    private final RequestDraftMemoryService draftMemoryService;

    // Сохранение заявки в базе данных
    public Request save(Request request) {
        return requestRepository.save(request);
    }

    // Получение всех заявок клиента
    public List<Request> findByClientId(Long clientId) {
        return requestRepository.findByClientId(clientId);
    }
    public void startDraft(Long chatId) {
        draftMemoryService.getDraft(chatId); // просто инициализирует
    }

    public void setType(Long chatId, String value) {
        draftMemoryService.getDraft(chatId).setType(value);
    }

    public void setDescription(Long chatId, String value) {
        draftMemoryService.getDraft(chatId).setDescription(value);
    }

    public void setDeadline(Long chatId, String value) {
        draftMemoryService.getDraft(chatId).setDeadline(value);
    }

    public void setBudget(Long chatId, String value) {
        draftMemoryService.getDraft(chatId).setBudget(value);
    }

    public void setContact(Long chatId, String value) {
        draftMemoryService.getDraft(chatId).setContact(value);
    }



    // Получение сводки черновика для отображения пользователю
    public String getDraftSummary(Long chatId) {
        RequestsDraft draft = draftMemoryService.getDraft(chatId);
        return String.format("""
            📝 Заявка:
            Тип: %s
            Описание: %s
            Сроки: %s
            Бюджет: %s
            Контакт: %s
            """, draft.getType(), draft.getDescription(), draft.getDeadline(), draft.getBudget(), draft.getContact());
    }

    public List<Request> getRequestsByClientId(Long clientId) {
        return requestRepository.findByClientId(clientId); // или другой идентификатор, как telegramId
    }

    // Обновление поля черновика
    public void updateDraftField(Long chatId, String field, String newValue) {
        RequestsDraft draft = draftMemoryService.getDraft(chatId);
        switch (field) {
            case "Тип проекта" -> draft.setType(newValue);
            case "Описание" -> draft.setDescription(newValue);
            case "Сроки" -> draft.setDeadline(newValue);
            case "Бюджет" -> draft.setBudget(newValue);
            case "Контакт" -> draft.setContact(newValue);
        }
    }

    // Сохранение черновика в финальную заявку
    public void saveFinalRequest(Long chatId) {

        RequestsDraft draft = draftMemoryService.getDraft(chatId);
        Request finalRequest = new Request();
        finalRequest.setType(draft.getType());
        finalRequest.setDescription(draft.getDescription());
        finalRequest.setDeadline(draft.getDeadline());
        finalRequest.setBudget(draft.getBudget());
        finalRequest.setContact(draft.getContact());
        finalRequest.setClientId(chatId); // или другое поле для идентификации клиента
        finalRequest.setStatus(RequestStatus.NEW); // ✅

//        finalRequest.setStatus("Новая");
        // attachments
//        List<Attachment> attachments = draft.getAttachmentFileIds().stream()
//                .map(fileId -> {
//                    Attachment attachment = new Attachment();
//                    attachment.setFileId(fileId);
//                    attachment.setRequest(finalRequest);
//                    return attachment;
//                })
//                .toList();
//        finalRequest.setAttachments(attachments);

        requestRepository.save(finalRequest);
        draftMemoryService.clearDraft(chatId);  // очищаем черновик
    }
//public boolean saveFinalRequest(Long chatId) {
//    RequestsDraft draft = draftMemoryService.getDraft(chatId);
//
//    if (!isValidDraft(draft)) {
//        return false; // не сохраняем
//    }
//
//    Request finalRequest = new Request();
//    finalRequest.setType(draft.getType());
//    finalRequest.setDescription(draft.getDescription());
//    finalRequest.setDeadline(draft.getDeadline());
//    finalRequest.setBudget(draft.getBudget());
//    finalRequest.setContact(draft.getContact());
//    finalRequest.setClientId(chatId);
//    finalRequest.setStatus(RequestStatus.NEW);
//
//    requestRepository.save(finalRequest);
//    draftMemoryService.clearDraft(chatId);
//    return true;
//}

    private boolean isValidDraft(RequestsDraft draft) {
        return draft != null
                && draft.getType() != null && !draft.getType().isBlank()
                && draft.getDescription() != null && !draft.getDescription().isBlank()
                && draft.getDeadline() != null
                && draft.getBudget() != null
                && draft.getContact() != null && !draft.getContact().isBlank();
    }

    // Получение черновика для данного чата
    public RequestsDraft getDraft(Long chatId) {
        return draftMemoryService.getDraft(chatId);
    }
}
