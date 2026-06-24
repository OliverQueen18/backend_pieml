package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ContactRequest;
import ml.gouv.pie.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final EmailService emailService;

    @Value("${app.contact.to:${app.mail.from:oliveservicespro@gmail.com}}")
    private String contactTo;

    public void sendContactMessage(ContactRequest request, User user) {
        String replyTo = request.getEmail().trim();
        String senderName = request.getName().trim();
        if (user != null) {
            replyTo = user.getEmail();
            if (senderName.isBlank() && user.getCitizen() != null) {
                senderName = user.getCitizen().getFirstName() + " " + user.getCitizen().getLastName();
            }
        }
        emailService.sendContactMessage(contactTo, replyTo, senderName, request.getSubject().trim(), request.getMessage().trim());
    }
}
