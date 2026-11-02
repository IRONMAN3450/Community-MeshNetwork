package org.example.mesh2.rescue;

import java.util.List;
import java.util.Optional;
import org.example.mesh2.sos.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescueAssignmentRepository extends JpaRepository<RescueAssignment, Long> {
    List<RescueAssignment> findByOrderByCreatedAtDesc();
    Optional<RescueAssignment> findFirstBySosAlertIdOrderByCreatedAtDesc(Long sosAlertId);
    long countByStatus(SosStatus status);
}
