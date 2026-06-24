package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.ApiResponse;
import ml.gouv.pie.dto.AuthResponse;
import ml.gouv.pie.dto.ChangePasswordRequest;
import ml.gouv.pie.dto.RequiredPasswordChangeRequest;
import ml.gouv.pie.entity.User;
import ml.gouv.pie.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Mot de passe modifié",
                accountService.changePassword(user, request)));
    }

    @PostMapping("/password/required")
    public ResponseEntity<ApiResponse<AuthResponse>> completeRequiredPasswordChange(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RequiredPasswordChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Mot de passe défini avec succès",
                accountService.completeRequiredPasswordChange(user, request)));
    }
}
