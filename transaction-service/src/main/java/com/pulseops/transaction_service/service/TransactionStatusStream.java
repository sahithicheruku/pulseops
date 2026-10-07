package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class TransactionStatusStream {
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID transactionId) {
        var emitter = new SseEmitter(30 * 60 * 1000L);
        var transactionEmitters = emitters.computeIfAbsent(transactionId, ignored -> new CopyOnWriteArrayList<>());
        transactionEmitters.add(emitter);
        Runnable remove = () -> remove(transactionId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());
        return emitter;
    }

    public void publish(TransactionStatusHistory history) {
        var transactionEmitters = emitters.get(history.getTransactionId());
        if (transactionEmitters == null) return;
        for (var emitter : transactionEmitters) {
            try {
                emitter.send(SseEmitter.event().name("status").data(history));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                remove(history.getTransactionId(), emitter);
            }
        }
    }

    private void remove(UUID transactionId, SseEmitter emitter) {
        var transactionEmitters = emitters.get(transactionId);
        if (transactionEmitters != null) {
            transactionEmitters.remove(emitter);
            if (transactionEmitters.isEmpty()) emitters.remove(transactionId, transactionEmitters);
        }
    }
}
