package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.InventaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventaires")
@RequiredArgsConstructor
public class InventaireController {

    private final InventaireService inventaireService;
    private final LocalRepository localRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Liste des inventaires
    @GetMapping
    public String liste(Model model) {
        model.addAttribute("inventaires",
            inventaireService.getTousLesInventaires());
        model.addAttribute("enCours",
            inventaireService.getInventairesEnCours());
        model.addAttribute("locaux", localRepository.findAll());
        return "inventaires/liste";
    }

    // Créer un inventaire
    @PostMapping("/creer")
    public String creer(@RequestParam Long localId,
                         @RequestParam String type,
                         Authentication auth,
                         RedirectAttributes ra) {
        try {
            Utilisateur utilisateur = utilisateurRepository
                .findByUsername(auth.getName()).orElseThrow();
            Inventaire inv = inventaireService.creerInventaire(
                localId, utilisateur.getId(), type);
            ra.addFlashAttribute("succes",
                "Inventaire créé ! Saisissez les quantités réelles.");
            return "redirect:/inventaires/saisie/" + inv.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/inventaires";
        }
    }

    // Page de saisie
    @GetMapping("/saisie/{id}")
    public String saisie(@PathVariable Long id, Model model) {
        model.addAttribute("inventaire",
            inventaireService.getInventaire(id));
        return "inventaires/saisie";
    }

    // Saisir une quantité réelle
    @PostMapping("/saisie/{inventaireId}/ligne/{ligneId}")
    public String saisirLigne(@PathVariable Long inventaireId,
                               @PathVariable Long ligneId,
                               @RequestParam Integer qteReelle,
                               RedirectAttributes ra) {
        try {
            inventaireService.saisirQteReelle(ligneId, qteReelle);
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/inventaires/saisie/" + inventaireId;
    }

    // Valider l'inventaire
    @PostMapping("/valider/{id}")
    public String valider(@PathVariable Long id,
                           @RequestParam(required = false)
                               String observations,
                           RedirectAttributes ra) {
        try {
            inventaireService.validerInventaire(id, observations);
            ra.addFlashAttribute("succes",
                "✅ Inventaire validé ! Stock mis à jour.");
            return "redirect:/inventaires/rapport/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/inventaires/saisie/" + id;
        }
    }

    // Rapport inventaire
    @GetMapping("/rapport/{id}")
    public String rapport(@PathVariable Long id, Model model) {
        model.addAttribute("inventaire",
            inventaireService.getInventaire(id));
        return "inventaires/rapport";
    }
}