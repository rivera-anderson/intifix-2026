package com.intifix.shared.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.intifix.modules.audit.event.CertificadoAprobadoEvent;
import com.intifix.modules.audit.event.CertificadoRechazadoEvent;
import com.intifix.modules.audit.event.TechnicianApprovedEvent;
import com.intifix.modules.audit.event.TechnicianRejectedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Puente entre los eventos internos de Spring y RabbitMQ. Los módulos del
 * monolito siguen publicando sus eventos igual que antes; esta clase los
 * reenvía a los microservicios interesados.
 *
 * <p>Se publica DESPUÉS del commit de la transacción: si la aprobación del
 * técnico hace rollback, no sale ninguna notificación. Si RabbitMQ no está
 * disponible se registra el error y la operación de negocio no falla
 * (siguiente mejora: patrón outbox para no perder eventos).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(fallbackExecution = true)
    public void tecnicoAprobado(TechnicianApprovedEvent evento) {
        publicar(Eventos.TECNICO_APROBADO, evento);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void tecnicoRechazado(TechnicianRejectedEvent evento) {
        publicar(Eventos.TECNICO_RECHAZADO, evento);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void certificadoAprobado(CertificadoAprobadoEvent evento) {
        publicar(Eventos.CERTIFICADO_APROBADO, evento);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void certificadoRechazado(CertificadoRechazadoEvent evento) {
        publicar(Eventos.CERTIFICADO_RECHAZADO, evento);
    }

    private void publicar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(Eventos.EXCHANGE, routingKey, evento);
            log.debug("Evento {} publicado", routingKey);
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} en RabbitMQ: {}", routingKey, e.getMessage());
        }
    }
}
