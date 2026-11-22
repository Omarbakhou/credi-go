package com.credigo.notification.repository;

import com.credigo.notification.entite.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop100ByOrderByIdDesc();
}
