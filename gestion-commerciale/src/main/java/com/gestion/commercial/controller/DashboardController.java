package com.gestion.commercial.controller;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final VenteRepository venteRepository;
    private final StockRepository stockRepository;
    private final ArticleRepository articleRepository;
    private final SessionJourneeRepository sessionRepository;
    private final MouvementStockRepository mouvementRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        LocalDateTime debutJour = LocalDateTime.now()
                .toLocalDate()
                .atStartOfDay();

        LocalDateTime finJour = debutJour.plusDays(1);

        // ==============================
        // VENTES DU JOUR
        // ==============================

        List<Vente> ventesJour = venteRepository.findAll()
                .stream()
                .filter(v ->
                        ("VALIDEE".equals(v.getStatut())
                        || "EN_ATTENTE_RETRAIT".equals(v.getStatut())
                        || "LIVREE".equals(v.getStatut()))
                        && v.getDateVente().isAfter(debutJour)
                        && v.getDateVente().isBefore(finJour))
                .toList();

        BigDecimal chiffreJour = ventesJour.stream()
                .map(Vente::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ==============================
        // SESSIONS OUVERTES
        // ==============================

        List<SessionJournee> toutesSessionsOuvertes = sessionRepository
                .findAll()
                .stream()
                .filter(s -> "OUVERTE".equals(s.getStatut()))
                .collect(Collectors.toList());

        // ==============================
        // DEMANDES EN ATTENTE
        // ==============================

        long demandesEnAttente = mouvementRepository
                .findByStatut("EN_ATTENTE")
                .size();

        // ==============================
        // ALERTES STOCK
        // ==============================

        List<Stock> alertesStock = stockRepository
                .findByQuantiteLessThanEqual(5);

        long totalArticles = articleRepository.count();

        // ==============================
        // STATISTIQUES 7 JOURS
        // ==============================

        List<String> jours = new ArrayList<>();
        List<BigDecimal> montants = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 6; i >= 0; i--) {

            LocalDateTime debut = LocalDateTime.now()
                    .toLocalDate()
                    .minusDays(i)
                    .atStartOfDay();

            LocalDateTime fin = debut.plusDays(1);

            BigDecimal total = venteRepository.findAll()
                    .stream()
                    .filter(v ->
                            ("VALIDEE".equals(v.getStatut())
                            || "EN_ATTENTE_RETRAIT".equals(v.getStatut())
                            || "LIVREE".equals(v.getStatut()))
                            && v.getDateVente().isAfter(debut)
                            && v.getDateVente().isBefore(fin))
                    .map(Vente::getMontantTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            jours.add(debut.format(fmt));
            montants.add(total);
        }

        // ==============================
        // ATTRIBUTS VIEW
        // ==============================

        model.addAttribute("chiffreJour", chiffreJour);
        model.addAttribute("nbVentesJour", ventesJour.size());

        // ✅ Nouveau
        model.addAttribute("sessionsOuvertes",
                toutesSessionsOuvertes.size());

        model.addAttribute("detailSessionsOuvertes",
                toutesSessionsOuvertes);

        model.addAttribute("demandesEnAttente", demandesEnAttente);
        model.addAttribute("alertesStock", alertesStock);
        model.addAttribute("totalArticles", totalArticles);

        model.addAttribute("joursLabels", jours);
        model.addAttribute("montantsData", montants);

        return "dashboard";
    }
}