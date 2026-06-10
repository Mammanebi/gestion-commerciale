package com.gestion.commercial.controller;

import com.gestion.commercial.entity.Local;
import com.gestion.commercial.repository.LocalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/locaux")
@RequiredArgsConstructor
public class LocalAdminController {

    private final LocalRepository localRepository;

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String nom,
                           @RequestParam String type,
                           @RequestParam(required = false) String adresse,
                           RedirectAttributes ra) {
        Local local = new Local();
        local.setNom(nom);
        local.setType(type);
        local.setAdresse(adresse);
        localRepository.save(local);
        ra.addFlashAttribute("succes",
            type + " « " + nom + " » ajouté avec succès !");
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id,
                             RedirectAttributes ra) {
        try {
            localRepository.deleteById(id);
            ra.addFlashAttribute("succes", "Local supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur",
                "Impossible de supprimer — ce local est utilisé.");
        }
        return "redirect:/admin/utilisateurs";
    }
}