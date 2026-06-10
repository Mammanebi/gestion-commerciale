package com.gestion.commercial.repository;

import com.gestion.commercial.entity.InventaireLigne;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventaireLigneRepository
        extends JpaRepository<InventaireLigne, Long> {
    List<InventaireLigne> findByInventaireId(Long inventaireId);
}