package ml.gouv.pie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ml.gouv.pie.dto.*;
import ml.gouv.pie.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Inscription réussie. Vérifiez votre OTP.", authService.register(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Compte activé", authService.verifyOtp(request)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Un nouveau code de vérification a été envoyé par email.", null));
    }

    @PostMapping("/cancel-registration")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(@Valid @RequestBody ResendOtpRequest request) {
        authService.cancelPendingRegistration(request);
        return ResponseEntity.ok(ApiResponse.ok("Inscription annulée.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Si un compte existe, un code de réinitialisation a été envoyé par email.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe réinitialisé avec succès", null));
    }
}
