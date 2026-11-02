package org.example.mesh2.mesh;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeshMessageRepository extends JpaRepository<MeshMessage, Long> {
    List<MeshMessage> findTop100ByOrderByCreatedAtDesc();
}
