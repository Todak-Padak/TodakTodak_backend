package com.padaks.todaktodak.notification.repository;

import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByMemberAndCreatedAtAfter(Member member, LocalDateTime dateTime, Pageable pageable);
}