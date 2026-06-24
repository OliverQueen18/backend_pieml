package ml.gouv.pie.service;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.VehicleLookupRequest;
import ml.gouv.pie.entity.VehicleBrand;
import ml.gouv.pie.exception.BusinessException;
import ml.gouv.pie.repository.VehicleBrandRepository;
import ml.gouv.pie.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleBrandService {

    private final VehicleBrandRepository vehicleBrandRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<DtoMapper.VehicleLookupDto> getActiveBrands() {
        return vehicleBrandRepository.findActiveOrdered()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DtoMapper.VehicleLookupDto> getAllBrands() {
        return vehicleBrandRepository.findAllOrdered()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DtoMapper.VehicleLookupDto create(VehicleLookupRequest request) {
        if (vehicleBrandRepository.existsByCode(normalizeCode(request.getCode()))) {
            throw new BusinessException("Ce code de marque existe déjà");
        }

        VehicleBrand brand = VehicleBrand.builder()
                .code(normalizeCode(request.getCode()))
                .libelle(request.getLibelle().trim())
                .description(request.getDescription())
                .actif(request.isActif())
                .ordre(request.getOrdre())
                .build();

        return toDto(vehicleBrandRepository.save(brand));
    }

    @Transactional
    public DtoMapper.VehicleLookupDto update(Long id, VehicleLookupRequest request) {
        VehicleBrand brand = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Marque non trouvée", HttpStatus.NOT_FOUND));

        String code = normalizeCode(request.getCode());
        if (vehicleBrandRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException("Ce code de marque existe déjà");
        }

        brand.setCode(code);
        brand.setLibelle(request.getLibelle().trim());
        brand.setDescription(request.getDescription());
        brand.setActif(request.isActif());
        brand.setOrdre(request.getOrdre());

        return toDto(vehicleBrandRepository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        VehicleBrand brand = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Marque non trouvée", HttpStatus.NOT_FOUND));

        if (vehicleRepository.existsByBrandEntityId(id)) {
            brand.setActif(false);
            vehicleBrandRepository.save(brand);
            return;
        }

        vehicleBrandRepository.delete(brand);
    }

    @Transactional(readOnly = true)
    public VehicleBrand getActiveById(Long id) {
        VehicleBrand brand = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Marque non trouvée", HttpStatus.NOT_FOUND));

        if (!brand.isActif()) {
            throw new BusinessException("Cette marque n'est pas active");
        }
        return brand;
    }

    public DtoMapper.VehicleLookupDto toDto(VehicleBrand brand) {
        return DtoMapper.VehicleLookupDto.builder()
                .id(brand.getId())
                .code(brand.getCode())
                .libelle(brand.getLibelle())
                .description(brand.getDescription())
                .actif(brand.isActif())
                .ordre(brand.getOrdre())
                .build();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }
}
