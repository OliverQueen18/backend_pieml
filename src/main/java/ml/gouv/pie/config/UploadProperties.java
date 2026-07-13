package ml.gouv.pie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class UploadProperties {
    /**
     * Répertoire racine des fichiers (documents dossiers, réclamations profil…).
     * En production Ubuntu : /home/adminubuntu/OliveApps/uploads/PIEML
     * ou dans Docker : /app/uploads/PIEML monté en volume.
     */
    private String dir = "./uploads/PIEML";
}
