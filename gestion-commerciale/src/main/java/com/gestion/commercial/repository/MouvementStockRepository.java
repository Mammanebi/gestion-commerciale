package com.gestion.commercial.repository;

import com.gestion.commercial.entity.MouvementStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    List<MouvementStock> findByStatut(String statut);
    List<MouvementStock> findByStockLocalId(Long localId);
    List<MouvementStock> findByStockIdAndType(Long stockId, String type);
}