package com.gestion.commercial.repository;

import com.gestion.commercial.entity.BonLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BonLivraisonRepository
        extends JpaRepository<BonLivraison, Long> {

    Optional<BonLivraison> findByVenteId(Long venteId);
    Optional<BonLivraison> findByNumero(String numero);
    List<BonLivraison> findByStatut(String statut);
    List<BonLivraison> findByMagasinId(Long magasinId);
}