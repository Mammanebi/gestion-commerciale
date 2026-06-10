package com.gestion.commercial.repository;

import com.gestion.commercial.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByArticleIdAndLocalId(Long articleId, Long localId);
    List<Stock> findByLocalId(Long localId);
    List<Stock> findByQuantiteLessThanEqual(Integer seuil);
}