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

@Service
@RequiredArgsConstructor
public class ReceptionService {

    private final ReceptionRepository receptionRepository;
    private final ReceptionLigneRepository ligneRepository;
    private final ArticleRepository articleRepository;
    private final LocalRepository localRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockService stockService;

    // Créer ou récupérer la réception en cours pour un local
    @Transactional
    public Reception getOuCreerReceptionEnCours(Long localId,
                                                 Long utilisateurId) {
        List<Reception> enCours = receptionRepository
            .findByLocalIdAndStatut(localId, "EN_COURS");

        if (!enCours.isEmpty()) {
            return enCours.get(0);
        }

        Local local = localRepository.findById(localId)
            .orElseThrow(() -> new RuntimeException("Local introuvable"));
        Utilisateur utilisateur = utilisateurRepository
            .findById(utilisateurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Reception reception = new Reception();
        reception.setLocal(local);
        reception.setUtilisateur(utilisateur);
        reception.setStatut("EN_COURS");
        reception.setDateReception(LocalDateTime.now());
        return receptionRepository.save(reception);
    }

    // Ajouter un article au panier de réception
    @Transactional
    public ReceptionLigne ajouterLigne(Long receptionId,
                                        Long articleId,
                                        Integer quantite,
                                        BigDecimal prixAchat) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("Réception introuvable"));

        if (!reception.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Cette réception est déjà validée");
        }

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));

        ReceptionLigne ligne = new ReceptionLigne();
        ligne.setReception(reception);
        ligne.setArticle(article);
        ligne.setQuantite(quantite);
        ligne.setPrixAchat(prixAchat != null
            ? prixAchat : article.getPrixAchat());
        return ligneRepository.save(ligne);
    }

    // Supprimer une ligne du panier
    @Transactional
    public void supprimerLigne(Long ligneId) {
        ligneRepository.deleteById(ligneId);
    }

    // Valider la réception — met à jour les stocks
    @Transactional
    public Reception validerReception(Long receptionId,
                                       String reference,
                                       String fournisseur) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("Réception introuvable"));

        if (!reception.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Réception déjà validée");
        }

        if (reception.getLignes().isEmpty()) {
            throw new RuntimeException(
                "Impossible de valider une réception vide");
        }

        // Mettre à jour le stock pour chaque article
        for (ReceptionLigne ligne : reception.getLignes()) {
            stockService.ajouterStock(
                ligne.getArticle().getId(),
                reception.getLocal().getId(),
                ligne.getQuantite()
            );

            // Mettre à jour le prix d'achat si fourni
            if (ligne.getPrixAchat() != null) {
                Article article = ligne.getArticle();
                article.setPrixAchat(ligne.getPrixAchat());
                articleRepository.save(article);
            }
        }

        reception.setStatut("VALIDEE");
        reception.setReference(reference);
        reception.setFournisseur(fournisseur);
        reception.setDateReception(LocalDateTime.now());
        return receptionRepository.save(reception);
    }

    // Récupérer toutes les réceptions
    public List<Reception> getToutesLesReceptions() {
        return receptionRepository.findAll();
    }

    // Récupérer une réception par ID
    public Reception getReception(Long id) {
        return receptionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Réception introuvable"));
    }

    // Annuler une réception en cours
    @Transactional
    public void annulerReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("Réception introuvable"));
        if (!reception.getStatut().equals("EN_COURS")) {
            throw new RuntimeException("Impossible d'annuler une réception validée");
        }
        receptionRepository.delete(reception);
    }
}