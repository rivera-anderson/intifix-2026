package com.intifix.modules.audit.listener;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.intifix.modules.audit.entity.WebSocketAction;
import com.intifix.modules.audit.entity.WebSocketLogDocument;
import com.intifix.modules.audit.event.ChatMessageSentEvent;
import com.intifix.modules.audit.event.ChatSessionEvent;
import com.intifix.modules.audit.service.WebSocketLogService;
import com.intifix.shared.messaging.Eventos;

import lombok.RequiredArgsConstructor;

/**
 * Registra la actividad del chat en {@code websocket_logs}.
 *
 * <p>El chat y su WebSocket ahora viven en chat-notif-service, que publica esta
 * actividad por RabbitMQ. La auditoría sigue siendo del monolito: solo cambió
 * de dónde llegan los eventos (antes, eventos internos de Spring).</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuditListener {

    private final WebSocketLogService webSocketLogService;

    @RabbitListener(queues = Eventos.Q_AUDIT_CHAT_CONECTADA)
    public void onConnect(ChatSessionEvent event) {
        registrar(null, event.userId(), WebSocketAction.CONNECT);
    }

    @RabbitListener(queues = Eventos.Q_AUDIT_CHAT_DESCONECTADA)
    public void onDisconnect(ChatSessionEvent event) {
        registrar(null, event.userId(), WebSocketAction.DISCONNECT);
    }

    @RabbitListener(queues = Eventos.Q_AUDIT_CHAT_MENSAJE)
    public void onMessageSent(ChatMessageSentEvent event) {
        registrar(event.conversationId(), event.senderId(), WebSocketAction.MESSAGE_SENT);
    }

    private void registrar(UUID conversationId, UUID userId, WebSocketAction action) {
        WebSocketLogDocument log = WebSocketLogDocument.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .userId(userId)
                .action(action)
                .build();
        webSocketLogService.registrar(log);
    }
}
