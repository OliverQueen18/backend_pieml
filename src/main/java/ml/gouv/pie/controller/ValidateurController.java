package ml.gouv.pie.controller;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/validateur")
@RequiredArgsConstructor
public class ValidateurController {

    private final ValidationService validationService;

    @PostMapping("/dossiers/{id}/validate")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> validate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier validé", validationService.validateDossier(id)));
    }

    @PostMapping("/dossiers/{id}/reject")
    public ResponseEntity<ApiResponse<DtoMapper.DossierDto>> reject(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Dossier rejeté",
                validationService.rejectDossier(id, body.getOrDefault("reason", "Documents non conformes"))));
    }
}
