package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VenteService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final ArticleRepository articleRepository;
    private final ClientRepository clientRepository;
    private final StockService stockService;
    private final SessionJourneeService sessionService;
    private final BonLivraisonRepository bonLivraisonRepository;

    // =============================================
    // CRÉER UNE VENTE
    // =============================================
    @Transactional
    public Vente creerVente(Long localId, Long clientId) {
        SessionJournee session = sessionService.getSessionOuverte(localId);

        Vente vente = new Vente();
        vente.setSession(session);
        vente.setNumero(genererNumeroVente());
        vente.setDateVente(LocalDateTime.now());
        vente.setMontantTotal(BigDecimal.ZERO);
        vente.setStatut("EN_COURS");

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));
            vente.setClient(client);
        }

        return venteRepository.save(vente);
    }

    // =============================================
    // AJOUTER ARTICLE — détection stock insuffisant
    // =============================================
    @Transactional
    public LigneVente ajouterLigne(Long venteId,
                                    Long articleId,
                                    Integer quantite) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (!vente.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Cette vente n'est plus modifiable");
        }

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));

        Long localId = vente.getSession().getLocal().getId();

        // Vérifier stock boutique
        Optional<Stock> stockBoutique =
            stockService.getStockDisponible(articleId, localId);

        int dispoBoutique = stockBoutique
            .map(Stock::getQuantiteDisponible).orElse(0);

        if (dispoBoutique >= quantite) {
            // CAS NORMAL — stock boutique suffisant
            stockService.diminuerStock(articleId, localId, quantite);
        } else {
            // CAS SPÉCIAL — stock boutique insuffisant
            throw new StockInsuffisantException(
                articleId, localId, quantite, dispoBoutique);
        }

        LigneVente ligne = new LigneVente();
        ligne.setVente(vente);
        ligne.setArticle(article);
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(article.getPrixVente());
        ligne.setMontantLigne(
            article.getPrixVente().multiply(BigDecimal.valueOf(quantite)));

        ligneVenteRepository.save(ligne);
        recalculerTotal(vente);
        return ligne;
    }

    // =============================================
    // AJOUTER ARTICLE DEPUIS MAGASIN (One-Click)
    // =============================================
    
 // ✅ Ajouter ligne depuis magasin + créer bon de livraison
    @Transactional
    public LigneVente ajouterLigneDepuisMagasin(Long venteId,
                                                  Long articleId,
                                                  Integer quantite,
                                                  Long magasinSourceId) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));

        // Réserver le stock dans le magasin source
        stockService.reserverStock(articleId, magasinSourceId, quantite);

        // Récupérer le local source
        Local sourceLogistique = stockService
            .getStockDisponible(articleId, magasinSourceId)
            .map(Stock::getLocal)
            .orElseThrow(() -> new RuntimeException("Magasin source introuvable"));

        // Marquer la vente
        vente.setStatut("EN_ATTENTE_RETRAIT");
        vente.setSourceLogistique(sourceLogistique);
        vente.setMentionLivraison(
            "À retirer au : " + sourceLogistique.getNom()
            + " — " + sourceLogistique.getAdresse());
        venteRepository.save(vente);

        // ✅ Créer le bon de livraison
        BonLivraison bon = new BonLivraison();
        bon.setVente(vente);
        bon.setMagasin(sourceLogistique);
        bon.setStatut("EN_ATTENTE");
        bon.setNumero(genererNumeroBonLivraison());
        bon.setDateCreation(java.time.LocalDateTime.now());
        bonLivraisonRepository.save(bon);

        LigneVente ligne = new LigneVente();
        ligne.setVente(vente);
        ligne.setArticle(article);
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(article.getPrixVente());
        ligne.setMontantLigne(
            article.getPrixVente().multiply(
                java.math.BigDecimal.valueOf(quantite)));

        ligneVenteRepository.save(ligne);
        recalculerTotal(vente);
        return ligne;
    }

    // ✅ Confirmer livraison — stock sort physiquement
    @Transactional
    public Vente confirmerLivraison(Long venteId,
                                     Long magasinierId,
                                     String observations) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (!vente.getStatut().equals("EN_ATTENTE_RETRAIT")) {
            throw new RuntimeException(
                "Cette vente n'est pas en attente de retrait");
        }

        // Sortir physiquement le stock du magasin
        List<LigneVente> lignes =
            ligneVenteRepository.findByVenteId(venteId);
        for (LigneVente ligne : lignes) {
            stockService.confirmerSortieReservee(
                ligne.getArticle().getId(),
                vente.getSourceLogistique().getId(),
                ligne.getQuantite()
            );
        }

        vente.setStatut("LIVREE");
        venteRepository.save(vente);

        // ✅ Mettre à jour le bon de livraison
        bonLivraisonRepository.findByVenteId(venteId).ifPresent(bon -> {
            bon.setStatut("LIVRE");
            bon.setDateLivraison(java.time.LocalDateTime.now());
            bon.setObservations(observations);
            if (magasinierId != null) {
                // Charger le magasinier
                bon.setMagasinier(
                    new com.gestion.commercial.entity.Utilisateur());
                bon.getMagasinier().setId(magasinierId);
            }
            bonLivraisonRepository.save(bon);
        });

        return vente;
    }

    // Générer numéro bon livraison
    private String genererNumeroBonLivraison() {
        String date = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd"));
        long count = bonLivraisonRepository.count() + 1;
        return String.format("BL-%s-%04d", date, count);
    }
  /*  @Transactional
    public LigneVente ajouterLigneDepuisMagasin(Long venteId,
                                                  Long articleId,
                                                  Integer quantite,
                                                  Long magasinSourceId) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));

        // Réserver le stock dans le magasin source
        stockService.reserverStock(articleId, magasinSourceId, quantite);

        // Récupérer le local source
        Local sourceLogistique = stockService
            .getStockDisponible(articleId, magasinSourceId)
            .map(Stock::getLocal)
            .orElseThrow(() -> new RuntimeException("Magasin source introuvable"));

        // Marquer la vente comme "En attente de retrait"
        vente.setStatut("EN_ATTENTE_RETRAIT");
        vente.setSourceLogistique(sourceLogistique);
        vente.setMentionLivraison(
            "À retirer au : " + sourceLogistique.getNom()
            + " — " + sourceLogistique.getAdresse());
        venteRepository.save(vente);

        LigneVente ligne = new LigneVente();
        ligne.setVente(vente);
        ligne.setArticle(article);
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(article.getPrixVente());
        ligne.setMontantLigne(
            article.getPrixVente().multiply(BigDecimal.valueOf(quantite)));

        ligneVenteRepository.save(ligne);
        recalculerTotal(vente);
        return ligne;
    }

    // =============================================
    // CONFIRMER LIVRAISON (stock sort physiquement)
    // =============================================
    @Transactional
    public Vente confirmerLivraison(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (!vente.getStatut().equals("EN_ATTENTE_RETRAIT")) {
            throw new RuntimeException(
                "Cette vente n'est pas en attente de retrait");
        }

        List<LigneVente> lignes = ligneVenteRepository.findByVenteId(venteId);
        for (LigneVente ligne : lignes) {
            stockService.confirmerSortieReservee(
                ligne.getArticle().getId(),
                vente.getSourceLogistique().getId(),
                ligne.getQuantite()
            );
        }

        vente.setStatut("LIVREE");
        return venteRepository.save(vente);
    }*/

    // =============================================
    // SUPPRIMER UNE LIGNE
    // =============================================
    @Transactional
    public void supprimerLigne(Long ligneId) {
        LigneVente ligne = ligneVenteRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne introuvable"));

        Vente vente = ligne.getVente();

        // Remettre en stock
        stockService.ajouterStock(
            ligne.getArticle().getId(),
            vente.getSession().getLocal().getId(),
            ligne.getQuantite()
        );

        ligneVenteRepository.delete(ligne);
        recalculerTotal(vente);
    }

    // =============================================
    // VALIDER LA VENTE
    // =============================================
    @Transactional
    public Vente validerVente(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (!vente.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Cette vente n'est plus modifiable");
        }

        List<LigneVente> lignes = ligneVenteRepository.findByVenteId(venteId);
        if (lignes.isEmpty()) {
            throw new RuntimeException("Impossible de valider une vente vide");
        }

        vente.setStatut("VALIDEE");
        return venteRepository.save(vente);
    }

    // =============================================
    // ANNULER LA VENTE
    // =============================================
    @Transactional
    public Vente annulerVente(Long venteId) {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (!vente.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Impossible d'annuler cette vente");
        }

        List<LigneVente> lignes = ligneVenteRepository.findByVenteId(venteId);
        for (LigneVente ligne : lignes) {
            stockService.ajouterStock(
                ligne.getArticle().getId(),
                vente.getSession().getLocal().getId(),
                ligne.getQuantite()
            );
        }

        vente.setStatut("ANNULEE");
        return venteRepository.save(vente);
    }

    // =============================================
    // LECTURE
    // =============================================
    public List<LigneVente> getLignesVente(Long venteId) {
        return ligneVenteRepository.findByVenteId(venteId);
    }

    public List<Vente> getVentesParSession(Long sessionId) {
        return venteRepository.findBySessionId(sessionId);
    }

    // =============================================
    // UTILITAIRES PRIVÉS
    // =============================================
    private void recalculerTotal(Vente vente) {
        List<LigneVente> lignes =
            ligneVenteRepository.findByVenteId(vente.getId());
        BigDecimal total = lignes.stream()
            .map(LigneVente::getMontantLigne)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        vente.setMontantTotal(total);
        venteRepository.save(vente);
    }

    private String genererNumeroVente() {
        String date = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = venteRepository.count() + 1;
        return String.format("VTE-%s-%04d", date, count);
    }
}