package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/ventes")
@RequiredArgsConstructor
public class VenteController {

    private final VenteService venteService;
    private final SessionJourneeService sessionService;

    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final LocalRepository localRepository;
    private final ArticleRepository articleRepository;
    private final StockRepository stockRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final StockService stockService;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final SessionJourneeRepository sessionRepository;

    // =========================================================
    // PAGE PRINCIPALE VENTES
    // =========================================================

    @GetMapping
    public String index(Model model, Authentication auth) {

        Utilisateur utilisateur = utilisateurRepository
                .findByUsername(auth.getName())
                .orElseThrow();

        // =====================================================
        // CAISSIER → uniquement sa boutique
        // =====================================================

        if ("CAISSIER".equals(utilisateur.getRole())
                && utilisateur.getLocalAffecte() != null) {

            Long localId = utilisateur.getLocalAffecte().getId();

            // Vérifier si une session existe déjà
            try {

                SessionJournee sessionExistante =
                        sessionService.getSessionOuverte(localId);

                return "redirect:/ventes/caisse/"
                        + sessionExistante.getId();

            } catch (Exception e) {

                // Pas de session ouverte
                model.addAttribute(
                        "boutiqueAffectee",
                        utilisateur.getLocalAffecte());

                return "ventes/index";
            }
        }

        // =====================================================
        // ADMIN / RESPONSABLE
        // =====================================================

        List<Local> boutiques =
                localRepository.findByType("BOUTIQUE");

        List<SessionJournee> sessionsOuvertes =
                sessionRepository.findAll()
                        .stream()
                        .filter(s -> "OUVERTE".equals(s.getStatut()))
                        .toList();

        model.addAttribute("locaux", boutiques);
        model.addAttribute("sessionsOuvertes", sessionsOuvertes);

        return "ventes/index";
    }

    // =========================================================
    // OUVRIR SESSION
    // =========================================================

    @PostMapping("/session/ouvrir")
    public String ouvrirSession(@RequestParam Long localId,
                                @RequestParam BigDecimal montantOuverture,
                                Authentication auth,
                                RedirectAttributes ra) {

        try {

            // Vérifier session déjà ouverte
            try {

                SessionJournee sessionExistante =
                        sessionService.getSessionOuverte(localId);

                ra.addFlashAttribute(
                        "succes",
                        "Session déjà ouverte — Redirection vers la caisse.");

                return "redirect:/ventes/caisse/"
                        + sessionExistante.getId();

            } catch (Exception e) {
                // Pas de session ouverte
            }

            Utilisateur utilisateur = utilisateurRepository
                    .findByUsername(auth.getName())
                    .orElseThrow();

            // Vérifier restriction caissier
            if ("CAISSIER".equals(utilisateur.getRole())
                    && utilisateur.getLocalAffecte() != null
                    && !utilisateur.getLocalAffecte()
                            .getId()
                            .equals(localId)) {

                ra.addFlashAttribute(
                        "erreur",
                        "Vous ne pouvez ouvrir que votre boutique : "
                                + utilisateur.getLocalAffecte().getNom());

                return "redirect:/ventes";
            }

            SessionJournee session = sessionService.ouvrirSession(
                    utilisateur.getId(),
                    localId,
                    montantOuverture);

            return "redirect:/ventes/caisse/" + session.getId();

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());

            return "redirect:/ventes";
        }
    }

    // =========================================================
    // PAGE CAISSE
    // =========================================================

    @GetMapping("/caisse/{sessionId}")
    public String caisse(@PathVariable Long sessionId,
                         Model model) {

        model.addAttribute("sessionId", sessionId);

        model.addAttribute(
                "ventes",
                venteService.getVentesParSession(sessionId));

        model.addAttribute(
                "clients",
                clientRepository.findAll());

        model.addAttribute(
                "stocks",
                stockRepository.findAll());

        return "ventes/caisse";
    }

    // =========================================================
    // NOUVELLE VENTE
    // =========================================================

    @PostMapping("/nouvelle")
    public String nouvelleVente(@RequestParam Long sessionId,
                                @RequestParam(required = false) Long clientId,
                                RedirectAttributes ra) {

        try {

            SessionJournee session = sessionService
                    .getToutesLesSessions()
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .orElseThrow();

            Vente vente = venteService.creerVente(
                    session.getLocal().getId(),
                    clientId);

            return "redirect:/ventes/detail/" + vente.getId();

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());

            return "redirect:/ventes/caisse/" + sessionId;
        }
    }

    // =========================================================
    // DETAIL VENTE
    // =========================================================

    @GetMapping("/detail/{venteId}")
    public String detail(@PathVariable Long venteId,
                         Model model) {

        Vente vente = venteRepository
                .findById(venteId)
                .orElseThrow();

        Long localId =
                vente.getSession().getLocal().getId();

        model.addAttribute("vente", vente);

        model.addAttribute(
                "lignes",
                venteService.getLignesVente(venteId));

        model.addAttribute(
                "stocks",
                stockRepository.findByLocalId(localId));

        return "ventes/detail";
    }

    // =========================================================
    // AJOUTER ARTICLE
    // =========================================================

    @PostMapping("/detail/{venteId}/ajouter")
    public String ajouterArticle(@PathVariable Long venteId,
                                 @RequestParam Long articleId,
                                 @RequestParam Integer quantite,
                                 RedirectAttributes ra) {

        try {

            venteService.ajouterLigne(
                    venteId,
                    articleId,
                    quantite);

        } catch (StockInsuffisantException e) {

            List<Stock> magasinsDisponibles =
                    stockService.getMagasinsAvecStock(
                            e.getArticleId(),
                            e.getQuantiteDemandee());

            if (!magasinsDisponibles.isEmpty()) {

                ra.addFlashAttribute("alerteStock", true);
                ra.addFlashAttribute("articleId", e.getArticleId());
                ra.addFlashAttribute("quantite", e.getQuantiteDemandee());
                ra.addFlashAttribute("dispoBoutique", e.getQuantiteDisponible());

                ra.addFlashAttribute(
                        "magasinsDisponibles",
                        magasinsDisponibles);

            } else {

                ra.addFlashAttribute(
                        "erreur",
                        "Stock insuffisant dans la boutique ET dans tous les magasins.");
            }

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());
        }

        return "redirect:/ventes/detail/" + venteId;
    }

    // =========================================================
    // AJOUTER DEPUIS MAGASIN
    // =========================================================

    @PostMapping("/detail/{venteId}/ajouter-magasin")
    public String ajouterDepuisMagasin(@PathVariable Long venteId,
                                       @RequestParam Long articleId,
                                       @RequestParam Integer quantite,
                                       @RequestParam Long magasinSourceId,
                                       RedirectAttributes ra) {

        try {

            venteService.ajouterLigneDepuisMagasin(
                    venteId,
                    articleId,
                    quantite,
                    magasinSourceId);

            ra.addFlashAttribute(
                    "succes",
                    "Article réservé au magasin. Facture avec mention de retrait.");

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());
        }

        return "redirect:/ventes/detail/" + venteId;
    }

    // =========================================================
    // CONFIRMER LIVRAISON
    // =========================================================

    @PostMapping("/detail/{venteId}/confirmer-livraison")
    public String confirmerLivraison(@PathVariable Long venteId,
                                     @RequestParam(required = false) Long magasinierId,
                                     @RequestParam(required = false) String observations,
                                     Authentication auth,
                                     RedirectAttributes ra) {

        try {

            if (magasinierId == null) {

                Utilisateur utilisateur = utilisateurRepository
                        .findByUsername(auth.getName())
                        .orElseThrow();

                magasinierId = utilisateur.getId();
            }

            venteService.confirmerLivraison(
                    venteId,
                    magasinierId,
                    observations);

            ra.addFlashAttribute(
                    "succes",
                    "Livraison confirmée ! Stock mis à jour.");

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());
        }

        return "redirect:/ventes/detail/" + venteId;
    }

    // =========================================================
    // SUPPRIMER LIGNE
    // =========================================================

    @GetMapping("/detail/{venteId}/supprimer/{ligneId}")
    public String supprimerLigne(@PathVariable Long venteId,
                                 @PathVariable Long ligneId,
                                 RedirectAttributes ra) {

        venteService.supprimerLigne(ligneId);

        return "redirect:/ventes/detail/" + venteId;
    }

    // =========================================================
    // VALIDER VENTE
    // =========================================================

    @PostMapping("/detail/{venteId}/valider")
    public String valider(@PathVariable Long venteId,
                          RedirectAttributes ra) {

        try {

            Vente vente = venteService.validerVente(venteId);

            ra.addFlashAttribute(
                    "succes",
                    "Vente " + vente.getNumero() + " validée !");

            return "redirect:/ventes/caisse/"
                    + vente.getSession().getId();

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());

            return "redirect:/ventes/detail/" + venteId;
        }
    }

    // =========================================================
    // ANNULER VENTE
    // =========================================================

    @PostMapping("/detail/{venteId}/annuler")
    public String annuler(@PathVariable Long venteId,
                          RedirectAttributes ra) {

        Vente vente = venteService.annulerVente(venteId);

        ra.addFlashAttribute("erreur", "Vente annulée.");

        return "redirect:/ventes/caisse/"
                + vente.getSession().getId();
    }

    // =========================================================
    // CLOTURER SESSION
    // =========================================================

    @PostMapping("/session/cloturer/{sessionId}")
    public String cloturerSession(@PathVariable Long sessionId,
                                  RedirectAttributes ra) {

        try {

            sessionService.cloturerSession(sessionId);

            ra.addFlashAttribute(
                    "succes",
                    "Journée clôturée avec succès !");

        } catch (Exception e) {

            ra.addFlashAttribute("erreur", e.getMessage());
        }

        return "redirect:/ventes";
    }

    // =========================================================
    // BON LIVRAISON
    // =========================================================

    @GetMapping("/detail/{venteId}/livraison")
    public String voirLivraison(@PathVariable Long venteId,
                                RedirectAttributes ra) {

        return bonLivraisonRepository.findByVenteId(venteId)
                .map(bon ->
                        "redirect:/livraisons/detail/" + bon.getId())
                .orElseGet(() -> {

                    ra.addFlashAttribute(
                            "erreur",
                            "Aucun bon de livraison trouvé.");

                    return "redirect:/ventes/detail/" + venteId;
                });
    }
}