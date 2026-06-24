package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Notification;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void create(User user, String message, NotificationType type) {
        create(user, message, type, true);
    }

    @Transactional
    public void create(User user, String message, NotificationType type, boolean sendEmail) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .read(false)
                .build();
        notificationRepository.save(notification);
        if (sendEmail) {
            emailService.sendNotificationEmail(user, type, message);
        }
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.NotificationDto> getMyNotifications(User user) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> DtoMapper.NotificationDto.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .type(n.getType())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
