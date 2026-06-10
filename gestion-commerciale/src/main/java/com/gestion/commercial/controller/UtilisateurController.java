package com.gestion.commercial.controller;

import com.gestion.commercial.dto.NouvelUtilisateurDTO;
import com.gestion.commercial.dto.UtilisateurDTO;
import com.gestion.commercial.repository.LocalRepository;
import com.gestion.commercial.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final LocalRepository localRepository;
    
 
    

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("utilisateurs",
            utilisateurService.getTousLesUtilisateurs()
                .stream()
                .map(UtilisateurDTO::new)
                .collect(Collectors.toList()));
        model.addAttribute("dto", new NouvelUtilisateurDTO());
        model.addAttribute("tousLocaux", localRepository.findAll());

        // ✅ Passer des données simples — pas les entités complètes
        model.addAttribute("boutiquesJson",
            localRepository.findByType("BOUTIQUE").stream()
                .map(l -> java.util.Map.of("id", l.getId(), "nom", l.getNom()))
                .collect(Collectors.toList()));

        model.addAttribute("magasinsJson",
            localRepository.findByType("MAGASIN").stream()
                .map(l -> java.util.Map.of("id", l.getId(), "nom", l.getNom()))
                .collect(Collectors.toList()));

        return "admin/utilisateurs";
    }

    @PostMapping("/creer")
    public String creer(@ModelAttribute NouvelUtilisateurDTO dto,
                        RedirectAttributes ra) {
        try {
            utilisateurService.creerUtilisateur(dto);
            ra.addFlashAttribute("succes",
                "Utilisateur " + dto.getUsername() + " créé avec succès !");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            utilisateurService.toggleActif(id);
            ra.addFlashAttribute("succes", "Statut mis à jour.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }

    @PostMapping("/reinitialiser/{id}")
    public String reinitialiser(@PathVariable Long id,
                                 @RequestParam String nouveauMotDePasse,
                                 RedirectAttributes ra) {
        try {
            utilisateurService.reinitialiserMotDePasse(id, nouveauMotDePasse);
            ra.addFlashAttribute("succes", "Mot de passe réinitialisé !");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            utilisateurService.supprimer(id);
            ra.addFlashAttribute("succes", "Utilisateur supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs";
    }
}