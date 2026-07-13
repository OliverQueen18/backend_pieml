package ml.gouv.pie.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ml.gouv.pie.config.UploadProperties;
import ml.gouv.pie.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredFileService {

    private final UploadProperties uploadProperties;

    @PostConstruct
    public void ensureUploadRootExists() {
        try {
            Path root = uploadRoot();
            Files.createDirectories(root);
            if (!Files.isWritable(root)) {
                throw new IllegalStateException("Upload directory is not writable: " + root);
            }
            log.info("Répertoire de stockage PIEML prêt : {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le répertoire upload : "
                    + uploadProperties.getDir(), e);
        }
    }

    public Path uploadRoot() {
        return Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
    }

    public Path dossierDirectory(String referenceNumber, String... subdirectories) {
        Path dir = uploadRoot().resolve(Paths.get(referenceNumber));
        for (String subdirectory : subdirectories) {
            dir = dir.resolve(subdirectory);
        }
        return dir;
    }

    public String toStoredPath(Path absoluteFile) {
        return uploadRoot().relativize(absoluteFile.toAbsolutePath().normalize()).toString();
    }

    public Path resolve(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new BusinessException("Aucun fichier téléversé pour ce document", HttpStatus.NOT_FOUND);
        }

        Path direct = Paths.get(storedPath);
        if (direct.isAbsolute() && Files.isRegularFile(direct)) {
            return direct.normalize();
        }

        String normalized = storedPath.replace('\\', '/');
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        }

        Path underRoot = uploadRoot().resolve(Paths.get(normalized)).normalize();
        if (Files.isRegularFile(underRoot)) {
            return underRoot;
        }

        Path legacy = uploadRoot().resolve(storedPath).normalize();
        if (Files.isRegularFile(legacy)) {
            return legacy;
        }

        Path fromWorkingDir = Paths.get(storedPath).toAbsolutePath().normalize();
        if (Files.isRegularFile(fromWorkingDir)) {
            return fromWorkingDir;
        }

        return underRoot;
    }
}
