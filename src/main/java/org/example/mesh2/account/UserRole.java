package org.example.mesh2.account;

public enum UserRole {
    CITIZEN,
    RESPONDER,
    ADMIN;

    public static UserRole fromUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return CITIZEN;
        }
        String normalized = userType.trim().toUpperCase().replace(' ', '_');
        return switch (normalized) {
            case "RESPONDER", "RESCUE", "RESCUE_TEAM", "FIRST_RESPONDER" -> RESPONDER;
            case "ADMIN", "ADMINISTRATOR", "COORDINATOR" -> ADMIN;
            default -> CITIZEN;
        };
    }
}
