package ml.gouv.pie.service;

import ml.gouv.pie.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StoredFileService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
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
