package org.example.mesh2.rescue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescueTeamRepository extends JpaRepository<RescueTeam, Long> {
    List<RescueTeam> findByOrderByNameAsc();
    Optional<RescueTeam> findByNameIgnoreCase(String name);
    long countByStatusIgnoreCase(String status);
}
