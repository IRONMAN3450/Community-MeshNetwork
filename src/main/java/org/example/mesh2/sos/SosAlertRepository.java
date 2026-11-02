package org.example.mesh2.sos;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {
    List<SosAlert> findByOrderByCreatedAtDesc();
    List<SosAlert> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<SosAlert> findByStatusNotOrderByCreatedAtDesc(SosStatus status);
    List<SosAlert> findByStatusNotInOrderByCreatedAtDesc(Collection<SosStatus> statuses);
    long countByStatus(SosStatus status);
    long countByStatusNot(SosStatus status);
    long countByStatusNotIn(Collection<SosStatus> statuses);
    long countByIncidentId(Long incidentId);
    long countByIncidentIdAndStatusNotIn(Long incidentId, Collection<SosStatus> statuses);
}
