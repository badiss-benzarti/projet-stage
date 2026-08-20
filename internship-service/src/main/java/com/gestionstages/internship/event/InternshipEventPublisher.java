package com.gestionstages.internship.event;

import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.enums.InternshipStatus;
import com.gestionstages.internship.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

import static com.gestionstages.internship.enums.InternshipStatus.*;

/**
 * Traduit une transition en evenement RabbitMQ.
 *
 * Une panne du bus ne doit JAMAIS faire echouer la transition metier :
 * l'echec de publication est journalise, la transaction se poursuit. Un
 * stage approuve reste approuve meme si la notification se perd.
 */
@Slf4j
@Component
public class InternshipEventPublisher {

    /** Etat cible -> cle de routage. Les etats absents ne generent rien. */
    private static final Map<InternshipStatus, String> ROUTING = Map.of(
            SUBMITTED,  "stage.submitted",
            APPROVED,   "stage.approved",
            REJECTED,   "stage.rejected",
            ACCEPTED,   "stage.company.accepted",
            REFUSED,    "stage.company.refused",
            COMPLETED,  "stage.completed");

    private final RabbitTemplate rabbit;
    private final String exchange;

    public InternshipEventPublisher(RabbitTemplate rabbit,
                                    @Value("${app.rabbit.exchange}") String exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    public void publish(Internship internship, InternshipStatus from,
                        AuthenticatedUser actor, String comment) {

        String routingKey = ROUTING.get(internship.getStatus());
        if (routingKey == null) {
            return;
        }

        InternshipEvent event = new InternshipEvent(
                routingKey,
                internship.getId(),
                internship.getTitle(),
                from,
                internship.getStatus(),
                internship.getStudentId(),
                internship.getStudentName(),
                internship.getStudentEmail(),
                internship.getCompanyId(),
                internship.getCompanyName(),
                actor.fullName(),
                actor.role(),
                comment,
                Instant.now());

        try {
            rabbit.convertAndSend(exchange, routingKey, event);
            log.info("Evenement publie : {} (stage {})", routingKey, internship.getId());
        } catch (AmqpException e) {
            log.error("Publication RabbitMQ echouee pour {} (stage {}) : {}",
                    routingKey, internship.getId(), e.getMessage());
        }
    }
}
