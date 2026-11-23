package com.intifix.shared.messaging;

/**
 * Contrato de mensajería entre servicios (ver docs/eventos.md en la raíz del
 * monorepo). Exchange topic {@value #EXCHANGE}, mensajes en JSON. Los nombres
 * deben coincidir con los de chat-notif-service.
 */
public final class Eventos {

    private Eventos() {
    }

    public static final String EXCHANGE = "intifix.events";
    public static final String DEAD_LETTER_EXCHANGE = "intifix.events.dlx";

    // --- Publica el monolito ---
    public static final String TECNICO_APROBADO = "technician.approved";
    public static final String TECNICO_RECHAZADO = "technician.rejected";
    public static final String CERTIFICADO_APROBADO = "certificate.approved";
    public static final String CERTIFICADO_RECHAZADO = "certificate.rejected";

    // --- Publica chat-notif-service, consume la auditoría del monolito ---
    public static final String CHAT_MENSAJE_ENVIADO = "chat.message.sent";
    public static final String CHAT_SESION_CONECTADA = "chat.session.connected";
    public static final String CHAT_SESION_DESCONECTADA = "chat.session.disconnected";

    // --- Colas propias del monolito ---
    public static final String Q_AUDIT_CHAT_MENSAJE = "monolith.audit.chat.message.sent";
    public static final String Q_AUDIT_CHAT_CONECTADA = "monolith.audit.chat.session.connected";
    public static final String Q_AUDIT_CHAT_DESCONECTADA = "monolith.audit.chat.session.disconnected";
    public static final String Q_DEAD_LETTER = "monolith.dead-letter";
}
