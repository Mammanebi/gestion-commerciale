package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bons-sortie")
@RequiredArgsConstructor
public class BonSortieController {

    private final BonSortieRepository bonSortieRepository;
    private final MouvementStockRepository mouvementRepository;

    @GetMapping
    public String liste(Model model) {
        List<BonSortie> bons = bonSortieRepository.findAll();

        // Stats pour l'en-tête
        model.addAttribute("bons", bons);
        model.addAttribute("totalBons", bons.size());
        model.addAttribute("totalTransferts", bons.stream()
            .filter(b -> "APPROVISIONNEMENT_BOUTIQUE".equals(
                b.getMouvement().getTypeMouvement()))
            .count());
        model.addAttribute("totalLivraisons", bons.stream()
            .filter(b -> "LIVRAISON_CLIENT".equals(
                b.getMouvement().getTypeMouvement()))
            .count());

        // Demandes rejetées pour info
        model.addAttribute("rejets",
            mouvementRepository.findByStatut("REJETE"));

        return "bons/liste";
    }
}