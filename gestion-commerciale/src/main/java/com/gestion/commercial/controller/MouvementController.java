package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.MouvementStockService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mouvements")
@RequiredArgsConstructor
public class MouvementController {

    private final MouvementStockService mouvementService;
    private final StockRepository stockRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LocalRepository localRepository;  // ✅ NOUVEAU

    // Liste des demandes en attente + formulaire
    @GetMapping
    public String liste(Model model, Authentication auth) {
        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(auth.getName()).orElseThrow();

        model.addAttribute("demandes",
            mouvementService.getDemandesEnAttente());

        // Stocks — source de sortie (magasins uniquement)
        model.addAttribute("stocks",
            stockRepository.findAll().stream()
                .filter(s -> s.getLocal().getType().equals("MAGASIN")
                    && s.getQuantiteDisponible() > 0)
                .sorted((a, b) -> a.getArticle().getDesignation()
                    .compareToIgnoreCase(b.getArticle().getDesignation()))
                .collect(java.util.stream.Collectors.toList()));

        // ✅ Locaux destination — caissier voit uniquement SA boutique
        if ("CAISSIER".equals(utilisateur.getRole())
                && utilisateur.getLocalAffecte() != null) {
            model.addAttribute("locaux",
                List.of(utilisateur.getLocalAffecte()));
        } else {
            model.addAttribute("locaux", localRepository.findAll());
        }

        model.addAttribute("utilisateur", utilisateur);
        return "mouvements/liste";
    }
    
   /* @GetMapping
    public String liste(Model model) {
        model.addAttribute("demandes", mouvementService.getDemandesEnAttente());
        model.addAttribute("stocks", stockRepository.findAll());
        model.addAttribute("locaux", localRepository.findAll()); // ✅ NOUVEAU
        return "mouvements/liste";
    } */

 // Créer une demande de sortie
    @PostMapping("/demander")
    public String demander(@RequestParam Long stockId,
                           @RequestParam Integer quantite,
                           @RequestParam String motif,
                           @RequestParam(required = false) Long localDestinationId,
                           @RequestParam(required = false) String typeMouvement,
                           Authentication auth,
                           RedirectAttributes ra) {
        try {
            Utilisateur utilisateur = utilisateurRepository
                .findByUsername(auth.getName())
                .orElseThrow();
            mouvementService.demanderSortie(
                stockId, utilisateur.getId(), quantite, motif,
                localDestinationId, typeMouvement);
            ra.addFlashAttribute("succes",
                "Demande de sortie créée, en attente d'approbation.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/mouvements";
    }

    
    
    // Approuver une sortie
    @PostMapping("/approuver/{id}")
    public String approuver(@PathVariable Long id,
                            @RequestParam(required = false) String observation,
                            Authentication auth,
                            RedirectAttributes ra) {
        Utilisateur responsable = utilisateurRepository
            .findByUsername(auth.getName())
            .orElseThrow();
        BonSortie bon = mouvementService.approuverSortie(
            id, responsable.getId(), observation);
        ra.addFlashAttribute("succes",
            "Sortie approuvée ! Bon de sortie : " + bon.getNumero());
        return "redirect:/mouvements";
    }

    // Rejeter une sortie
    @PostMapping("/rejeter/{id}")
    public String rejeter(@PathVariable Long id,
                          @RequestParam String motif,
                          RedirectAttributes ra) {
        mouvementService.rejeterSortie(id, motif);
        ra.addFlashAttribute("erreur", "Demande rejetée.");
        return "redirect:/mouvements";
    }
}