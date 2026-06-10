package com.gestion.commercial.repository;

import com.gestion.commercial.entity.SessionJournee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessionJourneeRepository extends JpaRepository<SessionJournee, Long> {
    Optional<SessionJournee> findByLocalIdAndStatut(Long localId, String statut);
}