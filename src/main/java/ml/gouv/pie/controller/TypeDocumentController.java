package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.DtoMapper;
import ml.gouv.pie.dto.TypeDocumentRequest;
import ml.gouv.pie.service.TypeDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/type-documents")
@RequiredArgsConstructor
public class TypeDocumentController {

    private final TypeDocumentService typeDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DtoMapper.TypeDocumentDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(typeDocumentService.getAllTypes()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DtoMapper.TypeDocumentDto>> create(
            @Valid @RequestBody TypeDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Type document créé", typeDocumentService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DtoMapper.TypeDocumentDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody TypeDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Type document mis à jour", typeDocumentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        typeDocumentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Type document supprimé ou désactivé", null));
    }
}
