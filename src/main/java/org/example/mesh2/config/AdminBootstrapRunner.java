package org.example.mesh2.config;

import java.util.Locale;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.account.UserAccountRepository;
import org.example.mesh2.account.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserAccountRepository accounts;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String phone;

    public AdminBootstrapRunner(
            UserAccountRepository accounts,
            @Value("${community.mesh.admin.email:}") String email,
            @Value("${community.mesh.admin.password:}") String password,
            @Value("${community.mesh.admin.first-name:Administrator}") String firstName,
            @Value("${community.mesh.admin.last-name:}") String lastName,
            @Value("${community.mesh.admin.phone:}") String phone) {
        this.accounts = accounts;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
        this.firstName = firstName == null || firstName.isBlank() ? "Administrator" : firstName.trim();
        this.lastName = lastName == null ? "" : lastName.trim();
        this.phone = phone == null ? "" : phone.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password.isBlank()) {
            logger.info("Admin bootstrap is disabled; set MESH_ADMIN_PASSWORD to create the initial administrator.");
            return;
        }
        if (email.isBlank() || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("Admin bootstrap requires MESH_ADMIN_EMAIL and a password between 12 and 72 characters.");
        }

        var existingAccount = accounts.findByEmail(email);
        if (existingAccount.isPresent()) {
            UserAccount existing = existingAccount.get();
            if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("The configured admin email already belongs to a non-admin account; no role was changed.");
            }
            if (!passwordEncoder.matches(password, existing.getPasswordHash())) {
                throw new IllegalStateException("An administrator already exists for the configured email; bootstrap will not replace its password.");
            }
            logger.info("Configured administrator account is already present.");
            return;
        }

        UserAccount administrator = new UserAccount(
                firstName,
                lastName,
                email,
                phone,
                "Admin",
                passwordEncoder.encode(password));
        administrator.setRole(UserRole.ADMIN);
        accounts.save(administrator);
        logger.info("Provisioned the initial administrator account.");
    }
}
