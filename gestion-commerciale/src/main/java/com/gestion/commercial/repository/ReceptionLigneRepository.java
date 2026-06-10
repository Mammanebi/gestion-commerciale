package com.gestion.commercial.repository;

import com.gestion.commercial.entity.ReceptionLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceptionLigneRepository
        extends JpaRepository<ReceptionLigne, Long> {
    List<ReceptionLigne> findByReceptionId(Long receptionId);
}