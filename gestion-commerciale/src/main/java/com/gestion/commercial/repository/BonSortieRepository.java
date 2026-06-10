package com.gestion.commercial.repository;

import com.gestion.commercial.entity.BonSortie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BonSortieRepository extends JpaRepository<BonSortie, Long> {
    Optional<BonSortie> findByNumero(String numero);
}