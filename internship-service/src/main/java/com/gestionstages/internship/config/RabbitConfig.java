package com.gestionstages.internship.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publication des evenements du workflow.
 *
 * Echange de type TOPIC : le notification-service s'abonnera plus tard a
 * "stage.#" sans que ce service ait a le connaitre. C'est ce decouplage
 * qui rend la communication asynchrone utile plutot que decorative.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange stagesExchange(@Value("${app.rabbit.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);
        return template;
    }
}
