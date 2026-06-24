package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.TypeDocumentRequest;
import ml.gouv.pie.entity.TypeDocument;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.DocumentRepository;
import ml.gouv.pie.repository.TypeDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeDocumentService {

    private final TypeDocumentRepository typeDocumentRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<DtoMapper.TypeDocumentDto> getActiveTypes() {
        return typeDocumentRepository.findByActifTrueOrderByOrdreAscLibelleAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.TypeDocumentDto> getAllTypes() {
        return typeDocumentRepository.findAllByOrderByOrdreAscLibelleAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TypeDocument> getActiveEntities() {
        return typeDocumentRepository.findByActifTrueOrderByOrdreAscLibelleAsc();
    }

    @Transactional
    public DtoMapper.TypeDocumentDto create(TypeDocumentRequest request) {
        if (typeDocumentRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Ce code de type document existe déjà");
        }

        TypeDocument type = TypeDocument.builder()
                .code(request.getCode().trim().toUpperCase())
                .libelle(request.getLibelle().trim())
                .description(request.getDescription())
                .obligatoire(request.isObligatoire())
                .actif(request.isActif())
                .ordre(request.getOrdre())
                .build();

        return toDto(typeDocumentRepository.save(type));
    }

    @Transactional
    public DtoMapper.TypeDocumentDto update(Long id, TypeDocumentRequest request) {
        TypeDocument type = typeDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type document non trouvé", HttpStatus.NOT_FOUND));

        if (typeDocumentRepository.existsByCodeAndIdNot(request.getCode().trim().toUpperCase(), id)) {
            throw new BusinessException("Ce code de type document existe déjà");
        }

        type.setCode(request.getCode().trim().toUpperCase());
        type.setLibelle(request.getLibelle().trim());
        type.setDescription(request.getDescription());
        type.setObligatoire(request.isObligatoire());
        type.setActif(request.isActif());
        type.setOrdre(request.getOrdre());

        return toDto(typeDocumentRepository.save(type));
    }

    @Transactional
    public void delete(Long id) {
        TypeDocument type = typeDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type document non trouvé", HttpStatus.NOT_FOUND));

        if (documentRepository.existsByTypeDocumentId(id)) {
            type.setActif(false);
            typeDocumentRepository.save(type);
            return;
        }

        typeDocumentRepository.delete(type);
    }

    @Transactional(readOnly = true)
    public TypeDocument getActiveById(Long id) {
        TypeDocument type = typeDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type document non trouvé", HttpStatus.NOT_FOUND));

        if (!type.isActif()) {
            throw new BusinessException("Ce type de document n'est pas actif");
        }
        return type;
    }

    public DtoMapper.TypeDocumentDto toDto(TypeDocument type) {
        return DtoMapper.TypeDocumentDto.builder()
                .id(type.getId())
                .code(type.getCode())
                .libelle(type.getLibelle())
                .description(type.getDescription())
                .obligatoire(type.isObligatoire())
                .actif(type.isActif())
                .ordre(type.getOrdre())
                .build();
    }
}
