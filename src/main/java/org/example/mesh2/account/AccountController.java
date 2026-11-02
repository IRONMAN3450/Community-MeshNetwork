package org.example.mesh2.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.example.mesh2.account.AccountDtos.AccountResponse;
import org.example.mesh2.account.AccountDtos.LoginRequest;
import org.example.mesh2.account.AccountDtos.ProfileUpdateRequest;
import org.example.mesh2.account.AccountDtos.SignupRequest;
import org.example.mesh2.auth.AuthInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.signup(request));
    }

    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        accountService.logout(request.getHeader(AuthInterceptor.TOKEN_HEADER));
        return Map.of("message", "Logged out successfully.");
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return accountService.currentProfile();
    }

    @PutMapping("/me")
    public AccountResponse updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        return accountService.updateProfile(request);
    }
}
