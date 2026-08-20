package com.gestionstages.notification.dto;

import com.gestionstages.notification.entity.Notification;

public record NotificationDto(
        Long id,
        String eventType,
        String title,
        String message,
        Long internshipId,
        boolean read,
        String createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getEventType(), n.getTitle(), n.getMessage(),
                n.getInternshipId(), n.isRead(),
                n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
    }
}
