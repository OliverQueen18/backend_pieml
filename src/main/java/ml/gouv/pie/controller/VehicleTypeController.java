package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.VehicleLookupRequest;
import ml.gouv.pie.service.VehicleTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DtoMapper.VehicleLookupDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleTypeService.getAllTypes()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DtoMapper.VehicleLookupDto>> create(
            @Valid @RequestBody VehicleLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Type d'engin créé", vehicleTypeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DtoMapper.VehicleLookupDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Type d'engin mis à jour", vehicleTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Type d'engin supprimé ou désactivé", null));
    }
}
