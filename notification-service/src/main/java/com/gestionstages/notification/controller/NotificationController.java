package com.gestionstages.notification.controller;

import com.gestionstages.notification.dto.NotificationDto;
import com.gestionstages.notification.security.AuthenticatedUser;
import com.gestionstages.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Notifications de l'utilisateur connecte. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @GetMapping("/mine")
    public Page<NotificationDto> mine(@AuthenticationPrincipal AuthenticatedUser me,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return notifications.mine(me, pageable);
    }

    /** Compteur pour la pastille du frontend. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser me) {
        return Map.of("unread", notifications.unreadCount(me));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@AuthenticationPrincipal AuthenticatedUser me,
                                    @PathVariable Long id) {
        return notifications.markRead(me, id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedUser me) {
        return Map.of("updated", notifications.markAllRead(me));
    }
}
