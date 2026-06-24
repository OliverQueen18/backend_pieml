package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.TariffRequest;
import ml.gouv.pie.service.TariffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DtoMapper.TariffDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(tariffService.getAllTariffs()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.TariffDto>> create(
            @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tarif créé", tariffService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DtoMapper.TariffDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tarif mis à jour", tariffService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tariffService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tarif supprimé", null));
    }
}
