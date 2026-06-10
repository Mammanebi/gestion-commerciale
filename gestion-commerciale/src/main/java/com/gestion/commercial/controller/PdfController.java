package com.gestion.commercial.controller;

import com.gestion.commercial.entity.BonLivraison;
import com.gestion.commercial.repository.BonLivraisonRepository;
import com.gestion.commercial.service.PdfService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final BonLivraisonRepository bonLivraisonRepository;
    

    // Télécharger la facture
    @GetMapping("/facture/{venteId}")
    public ResponseEntity<byte[]> facture(@PathVariable Long venteId) throws Exception {
        byte[] pdf = pdfService.genererFacture(venteId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=facture-" + venteId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    // Télécharger le bordereau d'encaissement
    @GetMapping("/bordereau/{sessionId}")
    public ResponseEntity<byte[]> bordereau(@PathVariable Long sessionId) throws Exception {
        byte[] pdf = pdfService.genererBordereau(sessionId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bordereau-" + sessionId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    // Télécharger le bon de sortie
    @GetMapping("/bon-sortie/{bonId}")
    public ResponseEntity<byte[]> bonSortie(@PathVariable Long bonId) throws Exception {
        byte[] pdf = pdfService.genererBonSortie(bonId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-sortie-" + bonId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
 // Rapport mensuel
    @GetMapping("/rapport-mensuel")
    public ResponseEntity<byte[]> rapportMensuel(
            @RequestParam int annee,
            @RequestParam int mois) throws Exception {
        byte[] pdf = pdfService.genererRapportMensuel(annee, mois);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=rapport-" + annee + "-" + mois + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
    @GetMapping("/rapport-mensuel/page")
    public String pagerapport() {
        return "rapports/mensuel";
    }
    
    @GetMapping("/bon-reception-multiple")
    public ResponseEntity<byte[]> bonReceptionMultiple(
            HttpSession session) throws Exception {

        @SuppressWarnings("unchecked")
        java.util.List<Long> stockIds =
            (java.util.List<Long>) session.getAttribute("receptionStockIds");
        @SuppressWarnings("unchecked")
        java.util.List<Integer> quantites =
            (java.util.List<Integer>) session.getAttribute("receptionQuantites");

        if (stockIds == null || stockIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = pdfService.genererBonReceptionMultiple(stockIds, quantites);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-reception.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
 // Bon de réception
    @GetMapping("/bon-reception/{stockId}")
    public ResponseEntity<byte[]> bonReception(
            @PathVariable Long stockId,
            @RequestParam Integer quantite) throws Exception {
        byte[] pdf = pdfService.genererBonReception(stockId, quantite);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-reception-" + stockId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
 // ✅ Bon de réception depuis la BASE (historique — réimpression)
    @GetMapping("/bon-reception-groupe/{receptionId}")
    public ResponseEntity<byte[]> bonReceptionGroupe(
            @PathVariable Long receptionId) throws Exception {
        byte[] pdf = pdfService.genererBonReceptionGroupe(receptionId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=reception-" + receptionId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    // ✅ Bon de réception depuis la SESSION (impression immédiate)
    @GetMapping("/bon-reception-session")
    public ResponseEntity<byte[]> bonReceptionSession(
            jakarta.servlet.http.HttpSession session) throws Exception {

        @SuppressWarnings("unchecked")
        List<Long> stockIds = (List<Long>)
            session.getAttribute("receptionStockIds");
        @SuppressWarnings("unchecked")
        List<Integer> quantites = (List<Integer>)
            session.getAttribute("receptionQuantites");
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> panier =
            (List<java.util.Map<String, Object>>)
            session.getAttribute("receptionPanier");

        if (stockIds == null || stockIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = pdfService.genererBonReceptionDepuisSession(
            stockIds, quantites, panier);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-reception.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
    
    @GetMapping("/bon-livraison/{bonId}")
    public ResponseEntity<byte[]> bonLivraison(
            @PathVariable Long bonId) throws Exception {
        byte[] pdf = pdfService.genererBonLivraison(bonId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-livraison-" + bonId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
    
 // Bon de livraison depuis l'ID de la vente
    @GetMapping("/bon-livraison-par-vente/{venteId}")
    public ResponseEntity<byte[]> bonLivraisonParVente(
            @PathVariable Long venteId) throws Exception {

        BonLivraison bon = bonLivraisonRepository.findByVenteId(venteId)
            .orElseThrow(() -> new RuntimeException("Bon de livraison introuvable"));

        byte[] pdf = pdfService.genererBonLivraison(bon.getId());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=bon-livraison-" + venteId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
    
    @GetMapping("/inventaire/{inventaireId}")
    public ResponseEntity<byte[]> inventaire(
            @PathVariable Long inventaireId) throws Exception {
        byte[] pdf = pdfService.genererRapportInventaire(inventaireId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=inventaire-" + inventaireId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
    
}