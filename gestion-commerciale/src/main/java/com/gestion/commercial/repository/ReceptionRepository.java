package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Reception;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceptionRepository
        extends JpaRepository<Reception, Long> {
    List<Reception> findByLocalIdAndStatut(Long localId, String statut);
    List<Reception> findByStatut(String statut);
}