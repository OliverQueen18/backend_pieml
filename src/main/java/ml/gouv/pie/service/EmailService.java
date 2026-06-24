package ml.gouv.pie.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.entity.Citizen;
import ml.gouv.pie.entity.Center;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final QrCodeService qrCodeService;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public void sendOtpEmail(User user, String otp, String firstName) {
        String subject = "PIE ML — Code de vérification de votre compte";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">PIE ML — Immatriculation Mali</h2>
                  <p>Bonjour %s,</p>
                  <p>Voici votre code de vérification pour activer votre compte :</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#2ecc71">%s</p>
                  <p>Ce code expire dans %d minutes. Ne le partagez avec personne.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">Plateforme d'Immatriculation des Engins du Mali</p>
                </div>
                """.formatted(firstName, otp, otpExpirationMinutes);

        sendOtpHtml(user.getEmail(), subject, html);
    }

    public void sendPasswordResetEmail(User user, String otp, String firstName) {
        String subject = "PIE ML — Réinitialisation de votre mot de passe";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">PIE ML — Mot de passe oublié</h2>
                  <p>Bonjour %s,</p>
                  <p>Voici votre code pour réinitialiser votre mot de passe :</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#2ecc71">%s</p>
                  <p>Ce code expire dans %d minutes. Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">Plateforme d'Immatriculation des Engins du Mali</p>
                </div>
                """.formatted(firstName, otp, otpExpirationMinutes);

        sendOtpHtml(user.getEmail(), subject, html);
    }

    public void sendStaffTemporaryPasswordEmail(User user, String temporaryPassword, String displayName) {
        String subject = "PIE ML — Votre nouveau mot de passe temporaire";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">PIE ML — Mot de passe réinitialisé</h2>
                  <p>Bonjour %s,</p>
                  <p>Un administrateur a réinitialisé votre mot de passe. Voici votre mot de passe temporaire :</p>
                  <p style="font-size:22px;font-weight:bold;letter-spacing:2px;color:#2ecc71">%s</p>
                  <p><strong>Important :</strong> à votre prochaine connexion, vous devrez définir un nouveau mot de passe personnel.</p>
                  <p>Ne partagez ce mot de passe avec personne.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">Plateforme d'Immatriculation des Engins du Mali</p>
                </div>
                """.formatted(displayName, temporaryPassword);

        sendOtpHtml(user.getEmail(), subject, html);
    }

    public void sendContactMessage(String to, String replyTo, String senderName, String subject, String message) {
        String fullSubject = "PIE ML — Contact : " + subject;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">Nouveau message de contact</h2>
                  <p><strong>De :</strong> %s &lt;%s&gt;</p>
                  <p><strong>Sujet :</strong> %s</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:16px 0">
                  <p style="white-space:pre-wrap">%s</p>
                </div>
                """.formatted(senderName, replyTo, subject, escapeHtml(message));

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setReplyTo(replyTo);
            helper.setSubject(fullSubject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Message contact envoyé de {} à {}", replyTo, to);
        } catch (Exception e) {
            log.error("Échec envoi message contact : {}", e.getMessage());
            throw new BusinessException("Impossible d'envoyer votre message pour le moment. Réessayez plus tard.");
        }
    }

    @Async
    public void sendDossierSubmittedEmail(User user, String referenceNumber) {
        if (!mailEnabled) {
            log.debug("Mail désactivé — QR dossier {} pour {}", referenceNumber, user.getEmail());
            return;
        }

        String greeting = resolveGreeting(user);
        String trackUrl = qrCodeService.buildTrackUrl(referenceNumber);
        byte[] qrPng = qrCodeService.generatePng(trackUrl, 280);
        String subject = "PIE ML — Votre dossier " + referenceNumber + " a été enregistré";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">Demande enregistrée</h2>
                  <p>%s,</p>
                  <p>Votre demande d'immatriculation a bien été enregistrée sous le numéro :</p>
                  <p style="font-size:22px;font-weight:bold;color:#2ecc71;letter-spacing:1px">%s</p>
                  <p>Conservez ce QR code pour suivre l'avancement de votre dossier à tout moment :</p>
                  <p style="text-align:center;margin:24px 0">
                    <img src="cid:qrCode" alt="QR code dossier %s" width="200" height="200"
                         style="border:1px solid #e5e7eb;border-radius:8px;padding:8px">
                  </p>
                  <p style="text-align:center">
                    <a href="%s"
                       style="background:#2ecc71;color:white;padding:12px 24px;text-decoration:none;border-radius:6px">
                      Suivre mon dossier
                    </a>
                  </p>
                  <p style="color:#666;font-size:13px;margin-top:20px">
                    Vous pouvez aussi scanner le QR code depuis la page « Suivre un dossier » sur le site PIE ML.
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">PIE ML — République du Mali</p>
                </div>
                """.formatted(greeting, referenceNumber, referenceNumber, trackUrl);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline("qrCode", new ByteArrayResource(qrPng), "image/png");
            mailSender.send(mime);
            log.info("Email QR dossier {} envoyé à {}", referenceNumber, user.getEmail());
        } catch (Exception e) {
            log.error("Échec envoi email QR dossier {} : {}", referenceNumber, e.getMessage());
        }
    }

    @Async
    public void sendNotificationEmail(User user, NotificationType type, String message) {
        if (!mailEnabled) {
            log.debug("Mail désactivé — notification pour {} : {}", user.getEmail(), message);
            return;
        }

        String subject = subjectFor(type);
        String greeting = resolveGreeting(user);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">PIE ML — Immatriculation Mali</h2>
                  <p>%s,</p>
                  <p>%s</p>
                  <p style="margin-top:24px">
                    <a href="http://localhost:4200/tableau-de-bord"
                       style="background:#2ecc71;color:white;padding:12px 24px;text-decoration:none;border-radius:6px">
                      Accéder à mon espace
                    </a>
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">Cet email a été envoyé automatiquement, merci de ne pas répondre.</p>
                </div>
                """.formatted(greeting, message);

        sendHtml(user.getEmail(), subject, html);
    }

    @Async
    public void sendAppointmentConvocation(User user, String referenceNumber, String centerName,
                                           String centerAddress, String date, String time) {
        if (!mailEnabled) return;

        String greeting = resolveGreeting(user);
        String subject = "PIE ML — Convocation pour votre rendez-vous (" + referenceNumber + ")";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">Convocation — Rendez-vous d'immatriculation</h2>
                  <p>%s,</p>
                  <p>Votre rendez-vous pour le dossier <strong>%s</strong> est confirmé :</p>
                  <ul>
                    <li><strong>Centre :</strong> %s</li>
                    <li><strong>Adresse :</strong> %s</li>
                    <li><strong>Date :</strong> %s</li>
                    <li><strong>Heure :</strong> %s</li>
                  </ul>
                  <p><strong>Important :</strong> présentez-vous avec vos documents originaux.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">PIE ML — République du Mali</p>
                </div>
                """.formatted(greeting, referenceNumber, centerName, centerAddress, date, time);

        sendHtml(user.getEmail(), subject, html);
    }

    @Async
    public void sendPaymentReceipt(User user, String referenceNumber, String transactionId,
                                   String amount, String paymentDate) {
        if (!mailEnabled) return;

        String greeting = resolveGreeting(user);
        String subject = "PIE ML — Reçu de paiement (" + referenceNumber + ")";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">Reçu de paiement</h2>
                  <p>%s,</p>
                  <p>Votre paiement a été confirmé avec succès.</p>
                  <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">N° dossier</td>
                        <td style="padding:8px;border-bottom:1px solid #eee"><strong>%s</strong></td></tr>
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">Transaction</td>
                        <td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">Montant</td>
                        <td style="padding:8px;border-bottom:1px solid #eee"><strong>%s FCFA</strong></td></tr>
                    <tr><td style="padding:8px">Date</td>
                        <td style="padding:8px">%s</td></tr>
                  </table>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">Trésor Pay — PIE ML</p>
                </div>
                """.formatted(greeting, referenceNumber, transactionId, amount, paymentDate);

        sendHtml(user.getEmail(), subject, html);
    }

    @Async
    public void sendCenterNewDossierNotification(List<User> staff, Dossier dossier, Center center) {
        if (!mailEnabled || staff == null || staff.isEmpty()) {
            return;
        }

        Citizen citizen = dossier.getCitizen();
        String citizenName = citizen != null
                ? citizen.getFirstName() + " " + citizen.getLastName()
                : "Citoyen";
        String vehicleSummary = buildVehicleSummary(dossier);
        String adminUrl = frontendUrl + "/admin/dossiers/" + dossier.getId();
        String subject = "PIE ML — Nouveau dossier payé (" + dossier.getReferenceNumber() + ")";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1e3a5f">Nouveau dossier à traiter</h2>
                  <p>Un citoyen a choisi votre centre <strong>%s — %s</strong> pour le traitement de sa demande.</p>
                  <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">N° dossier</td>
                        <td style="padding:8px;border-bottom:1px solid #eee"><strong>%s</strong></td></tr>
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">Citoyen</td>
                        <td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:8px;border-bottom:1px solid #eee">Engin</td>
                        <td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                    <tr><td style="padding:8px">Centre</td>
                        <td style="padding:8px">%s — %s</td></tr>
                  </table>
                  <p style="margin-top:24px">
                    <a href="%s"
                       style="background:#2ecc71;color:white;padding:12px 24px;text-decoration:none;border-radius:6px">
                      Ouvrir le dossier
                    </a>
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#888;font-size:12px">PIE ML — Notification automatique</p>
                </div>
                """.formatted(
                center.getCity(),
                center.getName(),
                dossier.getReferenceNumber(),
                escapeHtml(citizenName),
                escapeHtml(vehicleSummary),
                center.getCity(),
                center.getName(),
                adminUrl
        );

        for (User user : staff) {
            sendHtml(user.getEmail(), subject, html);
        }
    }

    private String buildVehicleSummary(Dossier dossier) {
        if (dossier.getVehicle() == null) {
            return "—";
        }
        var vehicle = dossier.getVehicle();
        String type = vehicle.getVehicleType() != null ? vehicle.getVehicleType() : "";
        String brand = vehicle.getBrand() != null ? vehicle.getBrand() : "";
        String model = vehicle.getModel() != null ? vehicle.getModel() : "";
        return (type + " " + brand + " " + model).trim();
    }

    private void sendOtpHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email OTP envoyé à {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Échec envoi email OTP à {} : {}", to, e.getMessage());
            throw new BusinessException(
                    "Impossible d'envoyer le code de vérification par email. Vérifiez votre adresse et réessayez.");
        }
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email envoyé à {} — {}", to, subject);
        } catch (MessagingException e) {
            log.error("Échec envoi email à {} : {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue envoi email à {} : {}", to, e.getMessage());
        }
    }

    private String resolveGreeting(User user) {
        Citizen citizen = user.getCitizen();
        if (citizen != null) {
            return "Bonjour " + citizen.getFirstName() + " " + citizen.getLastName();
        }
        return "Bonjour";
    }

    private String subjectFor(NotificationType type) {
        return switch (type) {
            case SUCCESS -> "PIE ML — Compte activé";
            case DOSSIER -> "PIE ML — Mise à jour de votre dossier";
            case PAYMENT -> "PIE ML — Confirmation de paiement";
            case APPOINTMENT -> "PIE ML — Rendez-vous confirmé";
            case WARNING -> "PIE ML — Action requise sur votre dossier";
            case INFO -> "PIE ML — Notification";
        };
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
