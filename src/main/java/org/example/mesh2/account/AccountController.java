package org.example.mesh2.account;

import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final UserAccountRepository accounts;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountController(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (blank(request.firstName()) || blank(request.lastName()) || blank(request.email()) ||
                blank(request.phone()) || blank(request.userType()) || request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please provide all required fields and a password of at least 6 characters."));
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "This email already has an account. Please log in."));
        }
        UserAccount account = accounts.save(new UserAccount(request.firstName().trim(), request.lastName().trim(), email,
                request.phone().trim(), request.userType(), passwordEncoder.encode(request.password())));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }
        return accounts.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(AccountResponse.from(account)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email or password is incorrect.")));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record SignupRequest(String firstName, String lastName, String email, String phone, String userType, String password) {}
    public record LoginRequest(String email, String password) {}
    public record AccountResponse(Long id, String firstName, String lastName, String email, String phone, String userType) {
        static AccountResponse from(UserAccount account) {
            return new AccountResponse(account.getId(), account.getFirstName(), account.getLastName(), account.getEmail(), account.getPhone(), account.getUserType());
        }
    }
}
