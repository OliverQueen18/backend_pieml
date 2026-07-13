package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.Document;
import ml.gouv.pie.entity.Dossier;
import ml.gouv.pie.entity.TypeDocument;
import ml.gouv.pie.entity.enums.DocumentStatus;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.entity.enums.NotificationType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.DocumentRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DossierService dossierService;
    private final DossierMapperService mapperService;
    private final TypeDocumentService typeDocumentService;
    private final StoredFileService storedFileService;
    private final NotificationService notificationService;

    @Transactional
    public DtoMapper.DossierDto uploadDocument(String email, Long dossierId, Long typeDocumentId, MultipartFile file) {
        Dossier dossier = dossierService.getOwnedDossier(email, dossierId);
        TypeDocument typeDocument = typeDocumentService.getActiveById(typeDocumentId);

        if (file.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }

        Document document = documentRepository.findByDossierIdAndTypeDocumentId(dossierId, typeDocumentId)
                .orElseThrow(() -> new BusinessException("Ce type de document n'est pas requis pour ce dossier"));

        ensureCitizenCanUploadDocument(dossier, document);

        try {
            if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
                Files.deleteIfExists(storedFileService.resolve(document.getFilePath()));
            }

            Path dir = storedFileService.dossierDirectory(dossier.getReferenceNumber());
            Files.createDirectories(dir);

            String ext = getExtension(file.getOriginalFilename());
            String storedName = typeDocument.getCode() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            document.setFileName(file.getOriginalFilename());
            document.setFilePath(storedFileService.toStoredPath(target));
            document.setFileSize(file.getSize());
            document.setContentType(file.getContentType());
            document.setStatus(DocumentStatus.UPLOADED);
            document.setUploadedAt(LocalDateTime.now());
            documentRepository.save(document);

        } catch (IOException e) {
            log.error("Échec téléversement dossier {} vers {} : {}",
                    dossierId, storedFileService.uploadRoot(), e.getMessage(), e);
            throw new BusinessException(
                    "Impossible d'enregistrer le fichier sur le serveur. Vérifiez UPLOAD_DIR et les droits d'écriture.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return mapperService.toDto(dossier);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    @Transactional(readOnly = true)
    public Document getOwnedDocument(String email, Long dossierId, Long documentId) {
        dossierService.getOwnedDossier(email, dossierId);
        return getDocumentForDossier(dossierId, documentId);
    }

    @Transactional(readOnly = true)
    public Document getDocumentForDossier(Long dossierId, Long documentId) {
        Document document = documentRepository.findByIdAndDossierId(documentId, dossierId)
                .orElseThrow(() -> new BusinessException("Document non trouvé", HttpStatus.NOT_FOUND));
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            throw new BusinessException("Aucun fichier téléversé pour ce document", HttpStatus.NOT_FOUND);
        }
        return document;
    }

    public ResponseEntity<Resource> buildFileResponse(Document document, Resource resource) {
        return ResponseEntity.ok()
                .contentType(resolveMediaType(document))
                .body(resource);
    }

    public ResponseEntity<Resource> buildFileResponse(String fileName, Resource resource, String contentType) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
    }

    private MediaType resolveMediaType(Document document) {
        try {
            return MediaType.parseMediaType(resolveContentType(document));
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource loadDocumentResource(Document document) {
        Path path = storedFileService.resolve(document.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("Fichier introuvable sur le serveur", HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    public String resolveContentType(Document document) {
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            return document.getContentType();
        }
        String fileName = document.getFileName();
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    @Transactional
    public DtoMapper.DossierDto validateDocuments(Long dossierId, List<Long> documentIds) {
        Dossier dossier = loadDossierWithDocuments(dossierId);
        ensureDocumentTreatmentAllowed(dossier);
        applyDocumentStatus(dossier, documentIds, DocumentStatus.VALIDATED);
        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto rejectDocuments(Long dossierId, List<Long> documentIds) {
        Dossier dossier = loadDossierWithDocuments(dossierId);
        ensureDocumentTreatmentAllowed(dossier);
        applyDocumentStatus(dossier, documentIds, DocumentStatus.REJECTED);
        return mapperService.toDto(dossier);
    }

    @Transactional
    public DtoMapper.DossierDto deleteDocument(Long dossierId, Long documentId) {
        Dossier dossier = loadDossierWithDocuments(dossierId);
        Document document = dossier.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Document non trouvé", HttpStatus.NOT_FOUND));

        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            try {
                Files.deleteIfExists(storedFileService.resolve(document.getFilePath()));
            } catch (IOException ignored) {
                // continue clearing metadata even if file removal fails
            }
        }
        document.setFileName(null);
        document.setFilePath(null);
        document.setFileSize(null);
        document.setContentType(null);
        document.setUploadedAt(null);
        document.setStatus(DocumentStatus.PENDING);
        documentRepository.save(document);

        notificationService.create(
                dossier.getCitizen().getUser(),
                "Le document « " + document.getTypeDocument().getLibelle() + " » du dossier "
                        + dossier.getReferenceNumber()
                        + " a été supprimé par l'administration. Merci de le téléverser à nouveau.",
                NotificationType.WARNING);

        return mapperService.toDto(dossier);
    }

    private void ensureCitizenCanUploadDocument(Dossier dossier, Document document) {
        DossierStatus dossierStatus = dossier.getStatus();
        if (dossierStatus == DossierStatus.DRAFT || dossierStatus == DossierStatus.REJECTED) {
            return;
        }
        if (dossierStatus == DossierStatus.PAID && document.getStatus() == DocumentStatus.REJECTED) {
            return;
        }
        throw new BusinessException("Ce dossier ne permet plus la modification des documents");
    }

    private Dossier loadDossierWithDocuments(Long dossierId) {
        return dossierService.getDossierEntity(dossierId);
    }

    private void ensureDocumentTreatmentAllowed(Dossier dossier) {
        if (dossier.getStatus() != DossierStatus.PAID) {
            throw new BusinessException("Les documents ne peuvent être traités que pour un dossier payé");
        }
    }

    private void applyDocumentStatus(Dossier dossier, List<Long> documentIds, DocumentStatus status) {
        List<String> labels = new ArrayList<>();
        for (Long documentId : documentIds) {
            Document document = dossier.getDocuments().stream()
                    .filter(d -> d.getId().equals(documentId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Document non trouvé : " + documentId, HttpStatus.NOT_FOUND));
            if (document.getFileName() == null || document.getFileName().isBlank()) {
                throw new BusinessException("Le document « " + document.getTypeDocument().getLibelle() + " » n'est pas téléversé");
            }
            document.setStatus(status);
            documentRepository.save(document);
            labels.add(document.getTypeDocument().getLibelle());
        }
        notifyDocumentStatusChange(dossier, status, labels);
    }

    private void notifyDocumentStatusChange(Dossier dossier, DocumentStatus status, List<String> documentLabels) {
        if (documentLabels.isEmpty()) {
            return;
        }
        String documents = String.join(", ", documentLabels);
        String reference = dossier.getReferenceNumber();
        if (status == DocumentStatus.VALIDATED) {
            notificationService.create(
                    dossier.getCitizen().getUser(),
                    "Document(s) validé(s) pour le dossier " + reference + " : " + documents + ".",
                    NotificationType.DOSSIER);
            return;
        }
        notificationService.create(
                dossier.getCitizen().getUser(),
                "Document(s) rejeté(s) pour le dossier " + reference + " : " + documents
                        + ". Merci de fournir un document conforme.",
                NotificationType.WARNING);
    }
}
