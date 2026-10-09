package com.mittiandmore.notification;

import com.mittiandmore.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailEnabled;

    public NotificationService(
        NotificationRepository notificationRepository,
        NotificationTemplateRepository templateRepository,
        JavaMailSender mailSender,
        @Value("${app.notifications.mail-enabled:false}") boolean mailEnabled,
        @Value("${app.notifications.from:}") String fromAddress
    ) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public void enqueue(NotificationEventType eventType, Order order, String trackingNumber, String trackingUrl) {
        if (
            order == null ||
            order.getCustomer() == null ||
            order.getCustomer().getEmail() == null ||
            order.getCustomer().getEmail().isBlank()
        ) return;
        NotificationTemplate template = templateRepository.findByEventType(eventType).orElse(null);
        if (template == null || !template.isEnabled()) return;

        Map<String, String> vars = Map.ofEntries(
            Map.entry("customerName", safe(order.getCustomer().getName())),
            Map.entry("orderNumber", safe(order.getOrderNumber())),
            Map.entry("total", money(order.getTotal())),
            Map.entry("paymentMethod", safe(order.getPaymentMethod())),
            Map.entry("status", safe(order.getOrderStatus())),
            Map.entry("trackingNumber", safe(trackingNumber)),
            Map.entry("trackingUrl", safe(trackingUrl))
        );
        Notification n = new Notification();
        n.setEventType(eventType);
        n.setRecipientEmail(order.getCustomer().getEmail().trim());
        n.setSubject(render(template.getSubjectTemplate(), vars));
        n.setBody(render(template.getBodyTemplate(), vars));
        n.setCustomerId(order.getCustomer().getId());
        n.setOrderId(order.getId());
        n.setStatus("PENDING");
        n.setLastError(
            mailEnabled ? null : "Email delivery is currently disabled; it will be sent after SMTP is enabled"
        );
        notificationRepository.save(n);
    }

    @Scheduled(fixedDelayString = "${app.notifications.poll-ms:30000}")
    @Transactional
    public void processQueue() {
        if (!mailEnabled) return;
        for (Notification n : claimBatch()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (fromAddress != null && !fromAddress.isBlank()) message.setFrom(fromAddress);
                message.setTo(n.getRecipientEmail());
                message.setSubject(n.getSubject());
                message.setText(n.getBody());
                mailSender.send(message);
                markSent(n.getId());
            } catch (Exception ex) {
                markFailed(n.getId(), ex.getMessage());
            }
        }
    }

    @Transactional
    protected java.util.List<Notification> claimBatch() {
        var rows = notificationRepository.findReady(LocalDateTime.now(), PageRequest.of(0, 20));
        for (Notification n : rows) {
            n.setStatus("SENDING");
            n.setAttemptCount(n.getAttemptCount() + 1);
            n.setNextAttemptAt(null);
        }
        return rows;
    }

    @Transactional
    protected void markSent(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setStatus("SENT");
            n.setSentAt(LocalDateTime.now());
            n.setLastError(null);
            n.setNextAttemptAt(null);
        });
    }

    @Transactional
    protected void markFailed(Long id, String error) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setStatus(n.getAttemptCount() >= 5 ? "FAILED_PERMANENT" : "FAILED");
            n.setLastError(
                error == null ? "Unknown mail delivery error" : error.substring(0, Math.min(1000, error.length()))
            );
            n.setNextAttemptAt(
                n.getAttemptCount() >= 5
                    ? null
                    : LocalDateTime.now().plusMinutes(Math.min(60, 5L * n.getAttemptCount()))
            );
        });
    }

    private static String render(String template, Map<String, String> vars) {
        String result = template;
        for (var e : vars.entrySet()) result = result.replace("{{" + e.getKey() + "}}", e.getValue());
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
