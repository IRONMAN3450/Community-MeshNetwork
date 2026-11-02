package org.example.mesh2.account;

import java.util.Locale;
import org.example.mesh2.account.AccountDtos.AccountResponse;
import org.example.mesh2.account.AccountDtos.LoginRequest;
import org.example.mesh2.account.AccountDtos.ProfileUpdateRequest;
import org.example.mesh2.account.AccountDtos.SignupRequest;
import org.example.mesh2.auth.AuthService;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.common.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserAccountRepository accounts;
    private final AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(UserAccountRepository accounts, AuthService authService) {
        this.accounts = accounts;
        this.authService = authService;
    }

    @Transactional
    public AccountResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (accounts.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "This email already has an account. Please log in.");
        }
        UserAccount account = accounts.save(new UserAccount(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone().trim(),
                "Citizen",
                passwordEncoder.encode(request.password())
        ));
        String token = authService.createSession(account);
        return AccountResponse.from(account, token);
    }

    @Transactional
    public AccountResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount account = accounts.findByEmail(email)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        String token = authService.createSession(account);
        return AccountResponse.from(account, token);
    }

    @Transactional
    public void logout(String token) {
        authService.logout(token);
    }

    @Transactional(readOnly = true)
    public AccountResponse currentProfile() {
        return AccountResponse.from(RequestContext.requireCurrentUser());
    }

    @Transactional
    public AccountResponse updateProfile(ProfileUpdateRequest request) {
        UserAccount account = RequestContext.requireCurrentUser();
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        if (notBlank(request.firstName())) {
            account.setFirstName(request.firstName().trim());
        }
        if (notBlank(request.lastName())) {
            account.setLastName(request.lastName().trim());
        }
        if (notBlank(request.phone())) {
            account.setPhone(request.phone().trim());
        }
        if (request.emergencyContactName() != null) {
            account.setEmergencyContactName(blankToNull(request.emergencyContactName()));
        }
        if (request.emergencyContactPhone() != null) {
            account.setEmergencyContactPhone(blankToNull(request.emergencyContactPhone()));
        }
        if (request.latitude() != null) {
            account.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            account.setLongitude(request.longitude());
        }
        return AccountResponse.from(accounts.save(account));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
