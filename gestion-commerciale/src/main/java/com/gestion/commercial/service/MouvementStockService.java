package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MouvementStockService {

    private final MouvementStockRepository mouvementRepository;
    private final BonSortieRepository bonSortieRepository;
    private final StockRepository stockRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LocalRepository localRepository;
    private final StockService stockService;

    // =============================================
    // DEMANDE DE SORTIE / TRANSFERT
    // =============================================
    @Transactional
    public MouvementStock demanderSortie(Long stockId,
                                          Long utilisateurId,
                                          Integer quantite,
                                          String motif,
                                          Long localDestinationId,
                                          String typeMouvement) {
        Stock stock = stockRepository.findById(stockId)
            .orElseThrow(() -> new RuntimeException("Stock introuvable"));
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (stock.getQuantite() < quantite) {
            throw new RuntimeException(
                "Stock insuffisant. Disponible : " + stock.getQuantite());
        }

        MouvementStock mouvement = new MouvementStock();
        mouvement.setStock(stock);
        mouvement.setUtilisateur(utilisateur);
        mouvement.setType("SORTIE");
        mouvement.setQuantite(quantite);
        mouvement.setMotif(motif);
        mouvement.setStatut("EN_ATTENTE");
        mouvement.setDateMouvement(LocalDateTime.now());

        // Type métier
        String typeMetier = typeMouvement != null ? typeMouvement
            : (localDestinationId != null
                ? "APPROVISIONNEMENT_BOUTIQUE" : "LIVRAISON_CLIENT");
        mouvement.setTypeMouvement(typeMetier);

        // Destination
        if (localDestinationId != null) {
            Local destination = localRepository.findById(localDestinationId)
                .orElseThrow(() -> new RuntimeException("Local destination introuvable"));
            mouvement.setLocalDestination(destination);
        }

        return mouvementRepository.save(mouvement);
    }

    // Ancienne signature conservée pour compatibilité
    @Transactional
    public MouvementStock demanderSortie(Long stockId,
                                          Long utilisateurId,
                                          Integer quantite,
                                          String motif) {
        return demanderSortie(stockId, utilisateurId, quantite, motif, null, "LIVRAISON_CLIENT");
    }

    // =============================================
    // APPROBATION — avec transfert automatique
    // =============================================
    @Transactional
    public BonSortie approuverSortie(Long mouvementId,
                                      Long responsableId,
                                      String observation) {
        MouvementStock mouvement = mouvementRepository.findById(mouvementId)
            .orElseThrow(() -> new RuntimeException("Mouvement introuvable"));
        Utilisateur responsable = utilisateurRepository.findById(responsableId)
            .orElseThrow(() -> new RuntimeException("Responsable introuvable"));

        if (!mouvement.getStatut().equals("EN_ATTENTE")) {
            throw new RuntimeException("Ce mouvement a déjà été traité");
        }

        // ✅ Déduire du stock source (magasin)
        stockService.diminuerStock(
            mouvement.getStock().getArticle().getId(),
            mouvement.getStock().getLocal().getId(),
            mouvement.getQuantite()
        );

        // ✅ Si c'est un transfert vers une boutique → augmenter le stock destination
        if ("APPROVISIONNEMENT_BOUTIQUE".equals(mouvement.getTypeMouvement())
                && mouvement.getLocalDestination() != null) {
            stockService.ajouterStock(
                mouvement.getStock().getArticle().getId(),
                mouvement.getLocalDestination().getId(),
                mouvement.getQuantite()
            );
        }

        // Mettre à jour le statut
        mouvement.setStatut("APPROUVE");
        mouvementRepository.save(mouvement);

        // Générer le bon de sortie
        BonSortie bon = new BonSortie();
        bon.setMouvement(mouvement);
        bon.setResponsable(responsable);
        bon.setNumero(genererNumeroBon());
        bon.setDateApprobation(LocalDateTime.now());
        bon.setObservation(observation);

        return bonSortieRepository.save(bon);
    }

    // =============================================
    // REJET
    // =============================================
    @Transactional
    public MouvementStock rejeterSortie(Long mouvementId, String motif) {
        MouvementStock mouvement = mouvementRepository.findById(mouvementId)
            .orElseThrow(() -> new RuntimeException("Mouvement introuvable"));

        if (!mouvement.getStatut().equals("EN_ATTENTE")) {
            throw new RuntimeException("Ce mouvement a déjà été traité");
        }

        mouvement.setStatut("REJETE");
        mouvement.setMotif(motif);
        return mouvementRepository.save(mouvement);
    }

    // =============================================
    // LECTURE
    // =============================================
    public List<MouvementStock> getDemandesEnAttente() {
        return mouvementRepository.findByStatut("EN_ATTENTE");
    }

    public List<MouvementStock> getTousLesMouvements() {
        return mouvementRepository.findAll();
    }

    // =============================================
    // UTILITAIRE
    // =============================================
    private String genererNumeroBon() {
        String date = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = bonSortieRepository.count() + 1;
        return String.format("BS-%s-%04d", date, count);
    }
}