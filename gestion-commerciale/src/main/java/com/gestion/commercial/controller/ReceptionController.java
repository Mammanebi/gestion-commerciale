package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/receptions")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;
    private final ArticleRepository articleRepository;
    private final LocalRepository localRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Page principale
    @GetMapping
    public String index(Model model) {
        model.addAttribute("receptions",
            receptionService.getToutesLesReceptions());
        model.addAttribute("magasins",
            localRepository.findByType("MAGASIN"));
        return "receptions/index";
    }

    // Ouvrir un panier de réception
    @PostMapping("/ouvrir")
    public String ouvrir(@RequestParam Long localId,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            Utilisateur utilisateur = utilisateurRepository
                .findByUsername(auth.getName()).orElseThrow();
            Reception reception = receptionService
                .getOuCreerReceptionEnCours(localId, utilisateur.getId());
            return "redirect:/receptions/panier/" + reception.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/receptions";
        }
    }

    // Panier de réception
    @GetMapping("/panier/{id}")
    public String panier(@PathVariable Long id, Model model) {
        Reception reception = receptionService.getReception(id);
        model.addAttribute("reception", reception);
        model.addAttribute("articles", articleRepository.findAll());
        return "receptions/panier";
    }

    // Ajouter un article au panier
    @PostMapping("/panier/{id}/ajouter")
    public String ajouterLigne(@PathVariable Long id,
                                @RequestParam Long articleId,
                                @RequestParam Integer quantite,
                                @RequestParam(required = false)
                                    BigDecimal prixAchat,
                                RedirectAttributes ra) {
        try {
            receptionService.ajouterLigne(id, articleId, quantite, prixAchat);
            ra.addFlashAttribute("succes", "Article ajouté au panier.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/receptions/panier/" + id;
    }

    // Supprimer une ligne
    @GetMapping("/panier/{receptionId}/supprimer/{ligneId}")
    public String supprimerLigne(@PathVariable Long receptionId,
                                  @PathVariable Long ligneId,
                                  RedirectAttributes ra) {
        receptionService.supprimerLigne(ligneId);
        return "redirect:/receptions/panier/" + receptionId;
    }

    // Valider la réception
    @PostMapping("/panier/{id}/valider")
    public String valider(@PathVariable Long id,
                           @RequestParam(required = false) String reference,
                           @RequestParam(required = false) String fournisseur,
                           RedirectAttributes ra) {
        try {
            receptionService.validerReception(id, reference, fournisseur);
            ra.addFlashAttribute("succes",
                "Réception validée ! Stock mis à jour.");
            return "redirect:/receptions/detail/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/receptions/panier/" + id;
        }
    }

    // Détail réception validée
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("reception", receptionService.getReception(id));
        return "receptions/detail";
    }

    // Annuler
    @GetMapping("/annuler/{id}")
    public String annuler(@PathVariable Long id, RedirectAttributes ra) {
        try {
            receptionService.annulerReception(id);
            ra.addFlashAttribute("succes", "Réception annulée.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/receptions";
    }
}