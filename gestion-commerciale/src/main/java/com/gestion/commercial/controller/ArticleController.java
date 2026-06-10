package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.gestion.commercial.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//Ajouter les imports nécessaires
import com.gestion.commercial.entity.Reception;
import com.gestion.commercial.entity.ReceptionLigne;
import com.gestion.commercial.repository.ReceptionRepository;
import com.gestion.commercial.repository.ReceptionLigneRepository;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final LocalRepository localRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final UtilisateurRepository utilisateurRepository;    // ✅ NOUVEAU
    private final ReceptionRepository receptionRepository;        // ✅ NOUVEAU
    private final ReceptionLigneRepository receptionLigneRepository; // ✅ NOUVEA

    // Liste de tous les articles
    @GetMapping
    public String liste(Model model) {
        model.addAttribute("articles", articleRepository.findAll());
        model.addAttribute("article", new Article());

        // ✅ Générer la prochaine référence automatique
        long count = articleRepository.count() + 1;
        String prochaineRef = String.format("ART%03d", count);
        model.addAttribute("prochaineRef", prochaineRef);

        return "articles/liste";
    }
    // Enregistrer un nouvel article
    @PostMapping("/save")
    public String save(@ModelAttribute Article article) {

        if(article.getId() == null) {

            article.setReference("TEMP");
            article = articleRepository.save(article);

            article.setReference(
                String.format("ART%04d", article.getId())
            );

            articleRepository.save(article);
        } else {
            articleRepository.save(article);
        }

        return "redirect:/articles";
    }

    // Formulaire de modification
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article introuvable"));
        model.addAttribute("article", article);
        model.addAttribute("articles", articleRepository.findAll());
        return "articles/liste";
    }

    // Supprimer un article
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        articleRepository.deleteById(id);
        ra.addFlashAttribute("succes", "Article supprimé.");
        return "redirect:/articles";
    }

    // Vue stock par local
    @GetMapping("/stock")
    public String stock(Model model,
                        HttpSession session,
                        Authentication auth) {
        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(auth.getName()).orElseThrow();

        List<Local> locaux;

        if ("CAISSIER".equals(utilisateur.getRole())
                && utilisateur.getLocalAffecte() != null) {
            // ✅ Caissier voit uniquement SA boutique
            locaux = List.of(utilisateur.getLocalAffecte());
        } else if ("MAGASINIER".equals(utilisateur.getRole())
                && utilisateur.getLocalAffecte() != null) {
            // ✅ Magasinier voit uniquement SON magasin
            locaux = List.of(utilisateur.getLocalAffecte());
        } else {
            // Admin / Responsable voient tout
            locaux = localRepository.findAll();
        }

        model.addAttribute("locaux", locaux);
        model.addAttribute("articles", articleRepository.findAll());

        List<java.util.Map<String, Object>> panier = getPanier(session);
        model.addAttribute("panier", panier);
        model.addAttribute("panierSize", panier.size());

        model.addAttribute("dernieresReceptions",
            receptionRepository.findAll().stream()
                .filter(r -> {
                    if ("CAISSIER".equals(utilisateur.getRole())
                            && utilisateur.getLocalAffecte() != null) {
                        return r.getStatut().equals("VALIDEE")
                            && r.getLocal().getId().equals(
                                utilisateur.getLocalAffecte().getId());
                    }
                    if ("MAGASINIER".equals(utilisateur.getRole())
                            && utilisateur.getLocalAffecte() != null) {
                        return r.getStatut().equals("VALIDEE")
                            && r.getLocal().getId().equals(
                                utilisateur.getLocalAffecte().getId());
                    }
                    return r.getStatut().equals("VALIDEE");
                })
                .sorted((a, b) -> b.getDateReception()
                    .compareTo(a.getDateReception()))
                .limit(5)
                .collect(java.util.stream.Collectors.toList()));

        return "articles/stock";
    }

    // ✅ Ajouter un article au panier (sans valider)
    @PostMapping("/stock/panier/ajouter")
    public String ajouterAuPanier(@RequestParam Long articleId,
                                   @RequestParam Long localId,
                                   @RequestParam Integer quantite,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        try {
            Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article introuvable"));
            Local local = localRepository.findById(localId)
                .orElseThrow(() -> new RuntimeException("Local introuvable"));

            List<Map<String, Object>> panier = getPanier(session);

            Map<String, Object> ligne = new java.util.LinkedHashMap<>();
            ligne.put("index", panier.size());
            ligne.put("articleId", articleId);
            ligne.put("localId", localId);
            ligne.put("articleNom", article.getDesignation());
            ligne.put("articleRef", article.getReference());
            ligne.put("localNom", local.getNom());
            ligne.put("quantite", quantite);
            ligne.put("unite", article.getUnite() != null
                ? article.getUnite() : "");
            panier.add(ligne);

            session.setAttribute("panierReception", panier);
            ra.addFlashAttribute("succes",
                "Article ajouté au panier (" + panier.size() + " article(s))");
        } catch (Exception e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/articles/stock";
    }

    // ✅ Supprimer une ligne du panier
    @GetMapping("/stock/panier/supprimer/{index}")
    public String supprimerDuPanier(@PathVariable int index,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        List<Map<String, Object>> panier = getPanier(session);
        if (index >= 0 && index < panier.size()) {
            panier.remove(index);
            // Recalculer les index
            for (int i = 0; i < panier.size(); i++) {
                panier.get(i).put("index", i);
            }
            session.setAttribute("panierReception", panier);
        }
        ra.addFlashAttribute("succes", "Article retiré du panier.");
        return "redirect:/articles/stock";
    }

    // ✅ Vider le panier
    @GetMapping("/stock/panier/vider")
    public String viderPanier(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute("panierReception");
        ra.addFlashAttribute("succes", "Panier vidé.");
        return "redirect:/articles/stock";
    }

 // ✅ Valider toute la réception du panier — sauvegarde en base
 // ✅ Valider réception — vérifier que le local appartient au rôle
    @PostMapping("/stock/panier/valider")
    public String validerReception(HttpSession session,
                                    Authentication auth,
                                    RedirectAttributes ra) {
        List<java.util.Map<String, Object>> panier = getPanier(session);
        if (panier.isEmpty()) {
            ra.addFlashAttribute("erreur", "Le panier est vide !");
            return "redirect:/articles/stock";
        }

        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(auth.getName()).orElseThrow();

        try {
            // Vérifier que chaque ligne correspond au local affecté
            for (java.util.Map<String, Object> ligne : panier) {
                Long localId = Long.valueOf(ligne.get("localId").toString());

                if (("CAISSIER".equals(utilisateur.getRole())
                        || "MAGASINIER".equals(utilisateur.getRole()))
                        && utilisateur.getLocalAffecte() != null
                        && !utilisateur.getLocalAffecte().getId()
                            .equals(localId)) {
                    ra.addFlashAttribute("erreur",
                        "Vous ne pouvez réceptionner que dans votre local : "
                        + utilisateur.getLocalAffecte().getNom());
                    session.removeAttribute("panierReception");
                    return "redirect:/articles/stock";
                }
            }

            // Traitement normal...
            List<Long> stockIds = new java.util.ArrayList<>();
            List<Integer> quantites = new java.util.ArrayList<>();
            com.gestion.commercial.entity.Reception reception =
                new com.gestion.commercial.entity.Reception();
            reception.setStatut("VALIDEE");
            reception.setDateReception(java.time.LocalDateTime.now());
            reception.setUtilisateur(utilisateur);

            Long premierLocalId = Long.valueOf(
                panier.get(0).get("localId").toString());
            com.gestion.commercial.entity.Local local =
                localRepository.findById(premierLocalId).orElseThrow();
            reception.setLocal(local);
            com.gestion.commercial.entity.Reception receptionSauvee =
                receptionRepository.save(reception);

            for (java.util.Map<String, Object> ligne : panier) {
                Long articleId = Long.valueOf(
                    ligne.get("articleId").toString());
                Long localId = Long.valueOf(ligne.get("localId").toString());
                Integer qte = Integer.valueOf(
                    ligne.get("quantite").toString());

                com.gestion.commercial.entity.Stock stock =
                    stockService.ajouterStock(articleId, localId, qte);
                stockIds.add(stock.getId());
                quantites.add(qte);

                com.gestion.commercial.entity.Article article =
                    articleRepository.findById(articleId).orElseThrow();
                com.gestion.commercial.entity.ReceptionLigne rl =
                    new com.gestion.commercial.entity.ReceptionLigne();
                rl.setReception(receptionSauvee);
                rl.setArticle(article);
                rl.setQuantite(qte);
                receptionLigneRepository.save(rl);
            }

            session.removeAttribute("panierReception");
            ra.addFlashAttribute("succes",
                "✅ Réception validée ! " + panier.size()
                + " article(s) reçu(s).");
            ra.addFlashAttribute("derniereReceptionId",
                receptionSauvee.getId());

        } catch (Exception e) {
            ra.addFlashAttribute("erreur", "Erreur : " + e.getMessage());
        }
        return "redirect:/articles/stock";
    }

    // ✅ Méthode utilitaire — récupérer le panier depuis la session
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPanier(HttpSession session) {
        List<Map<String, Object>> panier =
            (List<Map<String, Object>>) session.getAttribute("panierReception");
        if (panier == null) {
            panier = new ArrayList<>();
        }
        return panier;
    }
}