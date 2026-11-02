package org.example.mesh2.sos;

public enum SosStatus {
    RECEIVED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED;

    public SosStatus next() {
        return switch (this) {
            case RECEIVED -> ASSIGNED;
            case ASSIGNED -> IN_PROGRESS;
            case IN_PROGRESS -> RESOLVED;
            case RESOLVED, CANCELLED -> this;
        };
    }
}
