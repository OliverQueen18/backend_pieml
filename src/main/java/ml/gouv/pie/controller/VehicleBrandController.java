package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.VehicleLookupRequest;
import ml.gouv.pie.service.VehicleBrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-brands")
@RequiredArgsConstructor
public class VehicleBrandController {

    private final VehicleBrandService vehicleBrandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DtoMapper.VehicleLookupDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleBrandService.getAllBrands()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DtoMapper.VehicleLookupDto>> create(
            @Valid @RequestBody VehicleLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Marque créée", vehicleBrandService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DtoMapper.VehicleLookupDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Marque mise à jour", vehicleBrandService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleBrandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Marque supprimée ou désactivée", null));
    }
}
