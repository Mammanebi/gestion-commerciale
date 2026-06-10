package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final ArticleRepository articleRepository;
    private final LocalRepository localRepository;

    // Stock d'un local
    public List<Stock> getStockParLocal(Long localId) {
        return stockRepository.findByLocalId(localId);
    }

    // Stock en alerte
    public List<Stock> getStockEnAlerte() {
        return stockRepository.findByQuantiteLessThanEqual(5);
    }

    // Ajouter stock
    @Transactional
    public Stock ajouterStock(Long articleId, Long localId, Integer quantite) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));
        Local local = localRepository.findById(localId)
            .orElseThrow(() -> new RuntimeException("Local introuvable"));

        Stock stock = stockRepository
            .findByArticleIdAndLocalId(articleId, localId)
            .orElse(new Stock());

        stock.setArticle(article);
        stock.setLocal(local);
        stock.setQuantite(stock.getQuantite() == null ? quantite
            : stock.getQuantite() + quantite);
        if (stock.getQuantiteReservee() == null) stock.setQuantiteReservee(0);

        return stockRepository.save(stock);
    }

    // Diminuer stock
    @Transactional
    public Stock diminuerStock(Long articleId, Long localId, Integer quantite) {
        Stock stock = stockRepository
            .findByArticleIdAndLocalId(articleId, localId)
            .orElseThrow(() -> new RuntimeException("Stock introuvable"));

        if (stock.getQuantite() < quantite) {
            throw new RuntimeException(
                "Stock insuffisant. Disponible : " + stock.getQuantite());
        }
        stock.setQuantite(stock.getQuantite() - quantite);
        return stockRepository.save(stock);
    }

    // ✅ NOUVEAU — Réserver du stock (bloque sans diminuer)
    @Transactional
    public Stock reserverStock(Long articleId, Long localId, Integer quantite) {
        Stock stock = stockRepository
            .findByArticleIdAndLocalId(articleId, localId)
            .orElseThrow(() -> new RuntimeException("Stock introuvable"));

        int disponible = stock.getQuantiteDisponible();
        if (disponible < quantite) {
            throw new RuntimeException(
                "Stock disponible insuffisant. Disponible : " + disponible);
        }

        stock.setQuantiteReservee(
            (stock.getQuantiteReservee() != null
                ? stock.getQuantiteReservee() : 0) + quantite);
        return stockRepository.save(stock);
    }

    // ✅ NOUVEAU — Libérer une réservation (annulation)
    @Transactional
    public Stock libererReservation(Long articleId, Long localId, Integer quantite) {
        Stock stock = stockRepository
            .findByArticleIdAndLocalId(articleId, localId)
            .orElseThrow(() -> new RuntimeException("Stock introuvable"));

        int reservee = stock.getQuantiteReservee() != null
            ? stock.getQuantiteReservee() : 0;
        stock.setQuantiteReservee(Math.max(0, reservee - quantite));
        return stockRepository.save(stock);
    }

    // ✅ NOUVEAU — Confirmer la sortie réelle (réservation → sortie physique)
    @Transactional
    public Stock confirmerSortieReservee(Long articleId,
                                          Long localId,
                                          Integer quantite) {
        Stock stock = stockRepository
            .findByArticleIdAndLocalId(articleId, localId)
            .orElseThrow(() -> new RuntimeException("Stock introuvable"));

        if (stock.getQuantite() < quantite) {
            throw new RuntimeException("Stock physique insuffisant");
        }

        // Diminuer stock physique ET libérer réservation
        stock.setQuantite(stock.getQuantite() - quantite);
        int reservee = stock.getQuantiteReservee() != null
            ? stock.getQuantiteReservee() : 0;
        stock.setQuantiteReservee(Math.max(0, reservee - quantite));

        return stockRepository.save(stock);
    }

    // ✅ NOUVEAU — Vérifier stock dispo dans une boutique
    public Optional<Stock> getStockDisponible(Long articleId, Long localId) {
        return stockRepository.findByArticleIdAndLocalId(articleId, localId);
    }

    // ✅ NOUVEAU — Trouver le magasin avec le plus de stock d'un article
    public Optional<Stock> getMeilleurMagasinSource(Long articleId) {
        return stockRepository.findAll().stream()
            .filter(s -> s.getArticle().getId().equals(articleId)
                && s.getLocal().getType().equals("MAGASIN")
                && s.getQuantiteDisponible() > 0)
            .max((a, b) -> Integer.compare(
                a.getQuantiteDisponible(),
                b.getQuantiteDisponible()));
    }
 // ✅ Retourner TOUS les magasins ayant assez de stock
    public List<Stock> getMagasinsAvecStock(Long articleId, Integer quantite) {
        return stockRepository.findAll().stream()
            .filter(s -> s.getArticle().getId().equals(articleId)
                && s.getLocal().getType().equals("MAGASIN")
                && s.getQuantiteDisponible() >= quantite)
            .sorted((a, b) -> Integer.compare(
                b.getQuantiteDisponible(),
                a.getQuantiteDisponible()))
            .collect(java.util.stream.Collectors.toList());
    }
}