package org.example.mesh2.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AccountDtos {
    private AccountDtos() {}

    public record SignupRequest(
            @NotBlank(message = "First name is required.") String firstName,
            @NotBlank(message = "Last name is required.") String lastName,
            @NotBlank(message = "Email is required.") @Email(message = "Email is invalid.") String email,
            @NotBlank(message = "Phone is required.") @Size(max = 32, message = "Phone must be 32 characters or fewer.") String phone,
            @NotBlank(message = "User type is required.") String userType,
            @NotBlank(message = "Password is required.") @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters.") String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required.") String email,
            @NotBlank(message = "Password is required.") String password
    ) {}

    public record ProfileUpdateRequest(
            String firstName,
            String lastName,
            @Size(max = 32, message = "Phone must be 32 characters or fewer.") String phone,
            @Size(max = 120, message = "Emergency contact name must be 120 characters or fewer.") String emergencyContactName,
            @Size(max = 32, message = "Emergency contact phone must be 32 characters or fewer.") String emergencyContactPhone,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude
    ) {}

    public record AccountResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String userType,
            String role,
            String emergencyContactName,
            String emergencyContactPhone,
            Double latitude,
            Double longitude,
            String token
    ) {
        public static AccountResponse from(UserAccount account) {
            return from(account, null);
        }

        public static AccountResponse from(UserAccount account, String token) {
            return new AccountResponse(
                    account.getId(),
                    account.getFirstName(),
                    account.getLastName(),
                    account.getEmail(),
                    account.getPhone(),
                    account.getUserType(),
                    account.getRole().name(),
                    account.getEmergencyContactName(),
                    account.getEmergencyContactPhone(),
                    account.getLatitude(),
                    account.getLongitude(),
                    token
            );
        }
    }
}
