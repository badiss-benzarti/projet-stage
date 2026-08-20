package com.gestionstages.notification.service;

import com.gestionstages.notification.dto.NotificationDto;
import com.gestionstages.notification.entity.Notification;
import com.gestionstages.notification.exception.ApiExceptions;
import com.gestionstages.notification.repository.NotificationRepository;
import com.gestionstages.notification.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;

    @Transactional(readOnly = true)
    public Page<NotificationDto> mine(AuthenticatedUser me, Pageable pageable) {
        return notifications.findForRecipient(me.email(), me.role(), pageable)
                .map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthenticatedUser me) {
        return notifications.countUnread(me.email(), me.role());
    }

    /** On ne marque comme lue qu'une notification qui nous est adressee. */
    @Transactional
    public NotificationDto markRead(AuthenticatedUser me, Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Notification", id));

        boolean pourMoi = me.email().equalsIgnoreCase(n.getRecipientEmail())
                || me.role().equals(n.getRecipientRole());

        if (!pourMoi) {
            throw new ApiExceptions.ForbiddenException("Cette notification ne vous est pas adressee");
        }

        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
        }
        return NotificationDto.from(n);
    }

    @Transactional
    public int markAllRead(AuthenticatedUser me) {
        return notifications.markAllRead(me.email(), me.role(), Instant.now());
    }
}
