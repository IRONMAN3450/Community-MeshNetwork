package org.example.mesh2.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.account.UserAccountRepository;
import org.example.mesh2.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final long SESSION_DAYS = 7;

    private final AuthSessionRepository sessions;
    private final UserAccountRepository accounts;

    public AuthService(AuthSessionRepository sessions, UserAccountRepository accounts) {
        this.sessions = sessions;
        this.accounts = accounts;
    }

    @Transactional
    public String createSession(UserAccount account) {
        Instant now = Instant.now();
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.save(new AuthSession(token, account.getId(), now, now.plus(SESSION_DAYS, ChronoUnit.DAYS)));
        account.setLastSeenAt(now);
        accounts.save(account);
        return token;
    }

    @Transactional
    public UserAccount resolveUser(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        AuthSession session = sessions.findByToken(token.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again."));
        if (session.isExpired()) {
            sessions.delete(session);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        }
        return accounts.findById(session.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account not found. Please log in again."));
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.deleteByToken(token.trim());
        }
    }
}
