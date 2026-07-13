package ml.gouv.pie.controller;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.ContactRequest;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.entity.enums.DossierStatus;
import ml.gouv.pie.repository.DossierRepository;
import ml.gouv.pie.service.ContactService;
import ml.gouv.pie.service.TariffService;
import ml.gouv.pie.service.TypeDocumentService;
import ml.gouv.pie.service.VehicleBrandService;
import ml.gouv.pie.service.VehicleTypeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final DossierRepository dossierRepository;
    private final TypeDocumentService typeDocumentService;
    private final TariffService tariffService;
    private final ContactService contactService;
    private final VehicleBrandService vehicleBrandService;
    private final VehicleTypeService vehicleTypeService;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DtoMapper.PublicStatsDto>> getPublicStats() {
        long dossiersDeposes = dossierRepository.countAllDossiers();
        long dossiersValides = dossierRepository.countValidatedDossiers();
        long immatriculations = dossierRepository.countCompletedDossiers();
        long rejected = dossierRepository.countByStatus(DossierStatus.REJECTED);
        long decided = immatriculations + rejected;
        int satisfactionRate = decided == 0
                ? 0
                : (int) Math.round(100.0 * immatriculations / decided);

        DtoMapper.PublicStatsDto stats = DtoMapper.PublicStatsDto.builder()
                .dossiersDeposes(dossiersDeposes)
                .dossiersValides(dossiersValides)
                .immatriculations(immatriculations)
                .satisfactionRate(satisfactionRate)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/type-documents")
    public ResponseEntity<ApiResponse<List<DtoMapper.TypeDocumentDto>>> getTypeDocuments() {
        return ResponseEntity.ok(ApiResponse.ok(typeDocumentService.getActiveTypes()));
    }

    @GetMapping("/tariffs")
    public ResponseEntity<ApiResponse<List<DtoMapper.TariffDto>>> getTariffs() {
        return ResponseEntity.ok(ApiResponse.ok(tariffService.getActiveTariffs()));
    }

    @GetMapping("/vehicle-brands")
    public ResponseEntity<ApiResponse<List<DtoMapper.VehicleLookupDto>>> getVehicleBrands() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleBrandService.getActiveBrands()));
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<ApiResponse<List<DtoMapper.VehicleLookupDto>>> getVehicleTypes() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleTypeService.getActiveTypes()));
    }

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("version", appVersion)));
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Void>> contact(@Valid @RequestBody ContactRequest request) {
        contactService.sendContactMessage(request, null);
        return ResponseEntity.ok(ApiResponse.ok("Message envoyé. Nous vous répondrons sous 48 h.", null));
    }
}
