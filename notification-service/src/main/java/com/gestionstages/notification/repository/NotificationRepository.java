package com.gestionstages.notification.repository;

import com.gestionstages.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Ce qui m'est adresse nominativement, plus ce qui vise mon role. */
    @Query("select n from Notification n "
         + "where n.recipientEmail = :email or n.recipientRole = :role "
         + "order by n.createdAt desc")
    Page<Notification> findForRecipient(String email, String role, Pageable pageable);

    @Query("select count(n) from Notification n "
         + "where (n.recipientEmail = :email or n.recipientRole = :role) and n.read = false")
    long countUnread(String email, String role);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :moment "
         + "where (n.recipientEmail = :email or n.recipientRole = :role) and n.read = false")
    int markAllRead(String email, String role, Instant moment);

    Optional<Notification> findByIdAndRecipientEmail(Long id, String email);
}
