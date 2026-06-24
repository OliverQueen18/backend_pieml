package ml.gouv.pie.entity;

import jakarta.persistence.*;
import lombok.*;
import ml.gouv.pie.entity.enums.DocumentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dossier_id", "type_document_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_document_id", nullable = false)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String contentType;

    private LocalDateTime uploadedAt;
}
