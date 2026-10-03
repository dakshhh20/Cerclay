package com.mittiandmore.controller;

import com.mittiandmore.notification.Notification;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationRequest;
import com.mittiandmore.notification.NotificationService;
import com.mittiandmore.notification.NotificationTemplate;
import com.mittiandmore.notification.NotificationTemplateRepository;
import com.mittiandmore.notification.NotificationRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    public AdminNotificationController(NotificationTemplateRepository templateRepository, NotificationRepository notificationRepository){
        this.templateRepository=templateRepository; this.notificationRepository=notificationRepository;
    }
    @GetMapping("/templates") public ResponseEntity<List<NotificationTemplate>> templates(){return ResponseEntity.ok(templateRepository.findAll());}
    @GetMapping("/templates/events") public ResponseEntity<List<String>> events(){return ResponseEntity.ok(Arrays.stream(NotificationEventType.values()).map(Enum::name).toList());}
    @PutMapping("/templates/{eventType}") public ResponseEntity<NotificationTemplate> update(@PathVariable NotificationEventType eventType, @Valid @RequestBody NotificationRequest request){
        NotificationTemplate t=templateRepository.findByEventType(eventType).orElseGet(NotificationTemplate::new);
        t.setEventType(eventType); t.setEnabled(request.isEnabled()); t.setSubjectTemplate(request.getSubjectTemplate()); t.setBodyTemplate(request.getBodyTemplate());
        return ResponseEntity.ok(templateRepository.save(t));
    }
    @GetMapping("/history") public ResponseEntity<List<Notification>> history(){return ResponseEntity.ok(notificationRepository.findTop100ByOrderByCreatedAtDesc());}
}
