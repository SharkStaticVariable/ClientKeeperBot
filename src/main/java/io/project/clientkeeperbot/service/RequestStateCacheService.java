package io.project.clientkeeperbot.service;

import io.project.clientkeeperbot.entity.RequestStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestStateCacheService {

    private final Map<Long, Long> requestStateMap = new ConcurrentHashMap<>();

    private final Map<Long, RequestStatus> moderationStatusCache = new ConcurrentHashMap<>();

    public void saveRequestId(Long chatId, Long requestId) {
        requestStateMap.put(chatId, requestId);
    }

    public Long getRequestId(Long chatId) {
        return requestStateMap.get(chatId);
    }

    public void clearRequestId(Long chatId) {
        requestStateMap.remove(chatId);
    }
    public void saveModerationStatus(Long chatId, RequestStatus status) {
        moderationStatusCache.put(chatId, status);
    }

    public RequestStatus getModerationStatus(Long chatId) {
        return moderationStatusCache.get(chatId);
    }

    public void clearModerationStatus(Long chatId) {
        moderationStatusCache.remove(chatId);
    }
}
