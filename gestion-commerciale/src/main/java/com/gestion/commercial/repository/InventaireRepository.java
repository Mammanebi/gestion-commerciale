package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Inventaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventaireRepository
        extends JpaRepository<Inventaire, Long> {
    List<Inventaire> findByLocalId(Long localId);
    List<Inventaire> findByStatut(String statut);
}