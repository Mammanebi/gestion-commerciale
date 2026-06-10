package com.gestion.commercial.repository;

import com.gestion.commercial.entity.LigneVente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {

    List<LigneVente> findByVenteId(Long venteId);

    // Top articles vendus sur une période
    @Query("SELECT l.article.designation, SUM(l.quantite), SUM(l.montantLigne) " +
           "FROM LigneVente l " +
           "WHERE l.vente.statut = 'VALIDEE' " +
           "AND l.vente.dateVente >= :debut " +
           "AND l.vente.dateVente < :fin " +
           "GROUP BY l.article.designation " +
           "ORDER BY SUM(l.quantite) DESC")
    List<Object[]> findTopArticles(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin);
}