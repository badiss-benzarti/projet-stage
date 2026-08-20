package com.gestionstages.notification.listener;

import com.gestionstages.notification.entity.InternshipEvent;
import com.gestionstages.notification.repository.NotificationRepository;
import com.gestionstages.notification.service.NotificationComposer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consomme les evenements du workflow de stage.
 *
 * Un message qui ne produit aucune notification est acquitte quand meme :
 * le rejeter le ferait revenir en boucle dans la file.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternshipEventListener {

    private final NotificationComposer composer;
    private final NotificationRepository notifications;

    @RabbitListener(queues = "${app.rabbit.queue}")
    @Transactional
    public void onInternshipEvent(InternshipEvent event) {
        if (event == null || event.eventType() == null) {
            log.warn("Evenement ignore : payload vide ou sans type");
            return;
        }

        var produites = composer.compose(event);

        if (produites.isEmpty()) {
            log.debug("Evenement {} sans destinataire, ignore", event.eventType());
            return;
        }

        notifications.saveAll(produites);
        log.info("{} -> {} notification(s) pour le stage {}",
                event.eventType(), produites.size(), event.internshipId());
    }
}
