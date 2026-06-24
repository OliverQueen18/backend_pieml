package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.VehicleLookupRequest;
import ml.gouv.pie.entity.VehicleType;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.VehicleRepository;
import ml.gouv.pie.repository.VehicleTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<DtoMapper.VehicleLookupDto> getActiveTypes() {
        return vehicleTypeRepository.findActiveOrdered()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.VehicleLookupDto> getAllTypes() {
        return vehicleTypeRepository.findAllOrdered()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DtoMapper.VehicleLookupDto create(VehicleLookupRequest request) {
        if (vehicleTypeRepository.existsByCode(normalizeCode(request.getCode()))) {
            throw new BusinessException("Ce code de type d'engin existe déjà");
        }

        VehicleType type = VehicleType.builder()
                .code(normalizeCode(request.getCode()))
                .libelle(request.getLibelle().trim())
                .description(request.getDescription())
                .actif(request.isActif())
                .ordre(request.getOrdre())
                .build();

        return toDto(vehicleTypeRepository.save(type));
    }

    @Transactional
    public DtoMapper.VehicleLookupDto update(Long id, VehicleLookupRequest request) {
        VehicleType type = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type d'engin non trouvé", HttpStatus.NOT_FOUND));

        String code = normalizeCode(request.getCode());
        if (vehicleTypeRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException("Ce code de type d'engin existe déjà");
        }

        type.setCode(code);
        type.setLibelle(request.getLibelle().trim());
        type.setDescription(request.getDescription());
        type.setActif(request.isActif());
        type.setOrdre(request.getOrdre());

        return toDto(vehicleTypeRepository.save(type));
    }

    @Transactional
    public void delete(Long id) {
        VehicleType type = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type d'engin non trouvé", HttpStatus.NOT_FOUND));

        if (vehicleRepository.existsByVehicleTypeEntityId(id)) {
            type.setActif(false);
            vehicleTypeRepository.save(type);
            return;
        }

        vehicleTypeRepository.delete(type);
    }

    @Transactional(readOnly = true)
    public VehicleType getActiveById(Long id) {
        VehicleType type = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Type d'engin non trouvé", HttpStatus.NOT_FOUND));

        if (!type.isActif()) {
            throw new BusinessException("Ce type d'engin n'est pas actif");
        }
        return type;
    }

    public DtoMapper.VehicleLookupDto toDto(VehicleType type) {
        return DtoMapper.VehicleLookupDto.builder()
                .id(type.getId())
                .code(type.getCode())
                .libelle(type.getLibelle())
                .description(type.getDescription())
                .actif(type.isActif())
                .ordre(type.getOrdre())
                .build();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }
}
