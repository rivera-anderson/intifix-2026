package com.intifix.modules.audit.event;

import java.util.UUID;

/**
 * Un usuario abrió o cerró su sesión WebSocket del chat. Lo publica
 * chat-notif-service por RabbitMQ; lo consume la auditoría.
 */
public record ChatSessionEvent(UUID userId) {}
