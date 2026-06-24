package ml.gouv.pie.repository;

import ml.gouv.pie.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByDossierId(Long dossierId);
    Optional<Document> findByDossierIdAndTypeDocumentId(Long dossierId, Long typeDocumentId);
    boolean existsByTypeDocumentId(Long typeDocumentId);

    @Query("SELECT d FROM Document d WHERE d.id = :documentId AND d.dossier.id = :dossierId")
    Optional<Document> findByIdAndDossierId(@Param("documentId") Long documentId, @Param("dossierId") Long dossierId);
}
