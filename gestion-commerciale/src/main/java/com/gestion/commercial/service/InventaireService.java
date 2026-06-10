package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventaireService {

    private final InventaireRepository inventaireRepository;
    private final InventaireLigneRepository ligneRepository;
    private final LocalRepository localRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockRepository stockRepository;

    // Créer un inventaire — charge automatiquement le stock théorique
    @Transactional
    public Inventaire creerInventaire(Long localId,
                                       Long utilisateurId,
                                       String type) {
        Local local = localRepository.findById(localId)
            .orElseThrow(() -> new RuntimeException("Local introuvable"));
        Utilisateur utilisateur = utilisateurRepository
            .findById(utilisateurId)
            .orElseThrow(() -> new RuntimeException(
                "Utilisateur introuvable"));

        // Vérifier pas d'inventaire en cours pour ce local
        inventaireRepository.findByLocalId(localId).stream()
            .filter(i -> i.getStatut().equals("EN_COURS"))
            .findFirst()
            .ifPresent(i -> {
                throw new RuntimeException(
                    "Un inventaire est déjà en cours pour "
                    + local.getNom());
            });

        Inventaire inventaire = new Inventaire();
        inventaire.setLocal(local);
        inventaire.setUtilisateur(utilisateur);
        inventaire.setType(type);
        inventaire.setStatut("EN_COURS");
        inventaire.setDateInventaire(LocalDateTime.now());
        Inventaire inv = inventaireRepository.save(inventaire);

        // Charger automatiquement tous les articles du local
        List<Stock> stocks = stockRepository.findByLocalId(localId);
        for (Stock stock : stocks) {
            InventaireLigne ligne = new InventaireLigne();
            ligne.setInventaire(inv);
            ligne.setArticle(stock.getArticle());
            ligne.setQteTheorique(stock.getQuantite());
            ligne.setQteReelle(null); // à saisir
            ligne.setEcart(null);
            ligne.setValeurEcart(null);
            ligneRepository.save(ligne);
        }

        return inv;
    }

    // Saisir la quantité réelle d'une ligne
    @Transactional
    public InventaireLigne saisirQteReelle(Long ligneId,
                                            Integer qteReelle) {
        InventaireLigne ligne = ligneRepository.findById(ligneId)
            .orElseThrow(() -> new RuntimeException("Ligne introuvable"));

        ligne.setQteReelle(qteReelle);

        // Calculer l'écart
        int ecart = qteReelle - ligne.getQteTheorique();
        ligne.setEcart(ecart);

        // Valeur de l'écart
        BigDecimal prixUnitaire = ligne.getArticle().getPrixAchat() != null
            ? ligne.getArticle().getPrixAchat()
            : ligne.getArticle().getPrixVente();
        ligne.setValeurEcart(
            prixUnitaire.multiply(BigDecimal.valueOf(Math.abs(ecart))));

        return ligneRepository.save(ligne);
    }

    // Valider l'inventaire — met à jour le stock réel
    @Transactional
    public Inventaire validerInventaire(Long inventaireId,
                                         String observations) {
        Inventaire inventaire = inventaireRepository.findById(inventaireId)
            .orElseThrow(() -> new RuntimeException(
                "Inventaire introuvable"));

        if (!inventaire.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Cet inventaire est déjà validé");
        }

        // Vérifier toutes les lignes saisies
        long lignesNonSaisies = inventaire.getLignes().stream()
            .filter(l -> l.getQteReelle() == null)
            .count();
        if (lignesNonSaisies > 0) {
            throw new RuntimeException(
                lignesNonSaisies
                + " article(s) non saisi(s). "
                + "Veuillez saisir toutes les quantités.");
        }

        // Mettre à jour le stock avec les quantités réelles
        for (InventaireLigne ligne : inventaire.getLignes()) {
            stockRepository.findByArticleIdAndLocalId(
                    ligne.getArticle().getId(),
                    inventaire.getLocal().getId())
                .ifPresent(stock -> {
                    stock.setQuantite(ligne.getQteReelle());
                    stockRepository.save(stock);
                });
        }

        inventaire.setStatut("VALIDE");
        inventaire.setDateValidation(LocalDateTime.now());
        inventaire.setObservations(observations);
        return inventaireRepository.save(inventaire);
    }

    public Inventaire getInventaire(Long id) {
        return inventaireRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Inventaire introuvable"));
    }

    public List<Inventaire> getTousLesInventaires() {
        return inventaireRepository.findAll();
    }

    public List<Inventaire> getInventairesEnCours() {
        return inventaireRepository.findByStatut("EN_COURS");
    }
}