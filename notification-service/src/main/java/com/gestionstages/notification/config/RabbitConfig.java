package com.gestionstages.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Abonnement au flux d'evenements du workflow.
 *
 * La file est liee a l'echange avec la cle "stage.#" : ce service recoit
 * tous les evenements de stage, presents et futurs, sans que
 * internship-service ait a le connaitre. C'est ce decouplage qui rend la
 * communication asynchrone utile plutot que decorative.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange stagesExchange(@Value("${app.rabbit.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public Queue notificationsQueue(@Value("${app.rabbit.queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding binding(Queue notificationsQueue, TopicExchange stagesExchange,
                           @Value("${app.rabbit.binding-key}") String key) {
        return BindingBuilder.bind(notificationsQueue).to(stagesExchange).with(key);
    }

    /**
     * Le publieur estampille ses messages avec le nom de SA classe
     * (com.gestionstages.internship.event.InternshipEvent), qui n'existe
     * pas ici. TypePrecedence.INFERRED dit au convertisseur d'ignorer
     * cet en-tete et de se fier au type du parametre de la methode
     * ecoutante. Sans cela, chaque message part en erreur de
     * deserialisation.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper mapper = new DefaultJacksonJavaTypeMapper();
        mapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        mapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
