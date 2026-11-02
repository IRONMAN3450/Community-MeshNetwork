package org.example.mesh2.node;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkNodeRepository extends JpaRepository<NetworkNode, Long> {
    List<NetworkNode> findByOrderByCreatedAtDesc();
    Optional<NetworkNode> findByNodeCode(String nodeCode);
    long countByStatusIgnoreCase(String status);
}
