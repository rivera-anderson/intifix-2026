package com.intifix.shared.messaging;

import static com.intifix.shared.messaging.Eventos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Topología RabbitMQ del monolito: el exchange compartido y las colas de
 * auditoría del chat, cada una con dead-letter.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Declarables topologia() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetter = QueueBuilder.durable(Q_DEAD_LETTER).build();

        List<Declarable> declarables = new ArrayList<>(List.of(exchange, dlx, deadLetter,
                BindingBuilder.bind(deadLetter).to(dlx).with("monolith.#")));

        Map<String, String> colas = Map.of(
                Q_AUDIT_CHAT_MENSAJE, CHAT_MENSAJE_ENVIADO,
                Q_AUDIT_CHAT_CONECTADA, CHAT_SESION_CONECTADA,
                Q_AUDIT_CHAT_DESCONECTADA, CHAT_SESION_DESCONECTADA);
        colas.forEach((cola, routingKey) -> {
            Queue queue = QueueBuilder.durable(cola)
                    .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(cola)
                    .build();
            declarables.add(queue);
            declarables.add(BindingBuilder.bind(queue).to(exchange).with(routingKey));
        });
        return new Declarables(declarables);
    }

    /** JSON con el tipo deducido del parámetro del listener (sin nombres de clase en el mensaje). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
