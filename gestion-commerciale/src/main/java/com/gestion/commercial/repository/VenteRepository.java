package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    Optional<Vente> findByNumero(String numero);
    List<Vente> findBySessionId(Long sessionId);

    // Ventes entre deux dates
    @Query("SELECT v FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente >= :debut AND v.dateVente < :fin")
    List<Vente> findVentesValideesBetween(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin);
}