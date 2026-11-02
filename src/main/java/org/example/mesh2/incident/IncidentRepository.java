package org.example.mesh2.incident;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByOrderByCreatedAtDesc();
    List<Incident> findTop8ByStatusIgnoreCaseNotInOrderByCreatedAtDesc(List<String> statuses);
    List<Incident> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
    List<Incident> findByTypeIgnoreCaseOrderByCreatedAtDesc(String type);
    long countByStatusIgnoreCase(String status);
    long countByStatusIgnoreCaseNot(String status);
}
