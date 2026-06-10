package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.VenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/livraisons")
@RequiredArgsConstructor
public class BonLivraisonController {

    private final BonLivraisonRepository bonLivraisonRepository;
    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VenteService venteService;

    // ✅ Liste des bons de livraison — page magasinier
    @GetMapping
    public String liste(Model model) {
        model.addAttribute("enAttente",
            bonLivraisonRepository.findByStatut("EN_ATTENTE"));
        model.addAttribute("livres",
            bonLivraisonRepository.findByStatut("LIVRE"));
        return "livraisons/liste";
    }

    // ✅ Détail d'un bon de livraison
    @GetMapping("/detail/{bonId}")
    public String detail(@PathVariable Long bonId, Model model) {
        BonLivraison bon = bonLivraisonRepository.findById(bonId)
            .orElseThrow(() -> new RuntimeException("Bon introuvable"));
        model.addAttribute("bon", bon);
        model.addAttribute("lignes",
            ligneVenteRepository.findByVenteId(bon.getVente().getId()));
        return "livraisons/detail";
    }

    // ✅ Rechercher par numéro facture ou bon
    @GetMapping("/recherche")
    public String recherche(@RequestParam String numero, Model model) {
        // Chercher par numéro de bon livraison
        bonLivraisonRepository.findByNumero(numero).ifPresent(bon ->
            model.addAttribute("bon", bon));

        // Chercher par numéro de vente
        if (!model.containsAttribute("bon")) {
            venteRepository.findByNumero(numero).ifPresent(vente ->
                bonLivraisonRepository.findByVenteId(vente.getId())
                    .ifPresent(bon -> model.addAttribute("bon", bon)));
        }

        if (!model.containsAttribute("bon")) {
            model.addAttribute("erreur",
                "Aucun bon trouvé pour : " + numero);
        } else {
            BonLivraison bon = (BonLivraison) model.getAttribute("bon");
            model.addAttribute("lignes",
                ligneVenteRepository.findByVenteId(
                    bon.getVente().getId()));
        }

        model.addAttribute("enAttente",
            bonLivraisonRepository.findByStatut("EN_ATTENTE"));
        model.addAttribute("livres",
            bonLivraisonRepository.findByStatut("LIVRE"));
        model.addAttribute("numeroRecherche", numero);
        return "livraisons/liste";
    }

    // ✅ Confirmer la livraison
    @PostMapping("/confirmer/{bonId}")
    public String confirmer(@PathVariable Long bonId,
                             @RequestParam(required = false)
                                 String observations,
                             Authentication auth,
                             RedirectAttributes ra) {
        try {
            BonLivraison bon = bonLivraisonRepository.findById(bonId)
                .orElseThrow(() -> new RuntimeException("Bon introuvable"));

            Utilisateur magasinier = utilisateurRepository
                .findByUsername(auth.getName()).orElseThrow();

            venteService.confirmerLivraison(
                bon.getVente().getId(),
                magasinier.getId(),
                observations);

            ra.addFlashAttribute("succes",
                "✅ Livraison confirmée ! Stock débité.");
            return "redirect:/livraisons/detail/" + bonId;

        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/livraisons/detail/" + bonId;
        }
    }
}