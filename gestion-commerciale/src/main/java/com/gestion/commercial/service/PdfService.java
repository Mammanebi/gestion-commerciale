package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class PdfService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final BonSortieRepository bonSortieRepository;
    private final SessionJourneeRepository sessionRepository;
    private final StockRepository stockRepository;
    private final ReceptionRepository receptionRepository;
    private final ReceptionLigneRepository receptionLigneRepository;
    private final BonLivraisonRepository bonLivraisonRepository;
    private final InventaireRepository inventaireRepository;

    // Couleurs
    private static final BaseColor DARK_BLUE  = new BaseColor(26, 26, 46);
    private static final BaseColor LIGHT_GREY = new BaseColor(248, 249, 250);
    private static final BaseColor GREEN      = new BaseColor(39, 174, 96);
    private static final BaseColor WHITE      = BaseColor.WHITE;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =============================================
    // FACTURE
    // =============================================
    public byte[] genererFacture(Long venteId) throws Exception {
        Vente vente = venteRepository.findById(venteId)
            .orElseThrow(() -> new RuntimeException("Vente introuvable"));
        List<LigneVente> lignes = ligneVenteRepository.findByVenteId(venteId);

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Fonts
        Font fontTitre   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, WHITE);
        Font fontSousTitre = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
        Font fontLabel   = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
        Font fontValeur  = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
        Font fontHeader  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
        Font fontCell    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
        Font fontTotal   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);

        // ---- En-tête ----
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2f, 1f});

        PdfPCell cellSociete = new PdfPCell();
        cellSociete.setBackgroundColor(DARK_BLUE);
        cellSociete.setBorder(Rectangle.NO_BORDER);
        cellSociete.setPadding(15);
        Paragraph pSociete = new Paragraph("GESTION COMMERCIALE\n", fontTitre);
        pSociete.add(new Phrase("Matériels pour production et distribution d'eau", fontSousTitre));
        cellSociete.addElement(pSociete);
        header.addCell(cellSociete);

        PdfPCell cellFacture = new PdfPCell();
        cellFacture.setBackgroundColor(GREEN);
        cellFacture.setBorder(Rectangle.NO_BORDER);
        cellFacture.setPadding(15);
        cellFacture.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pFacture = new Paragraph("FACTURE\n", fontTitre);
        pFacture.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pNum = new Paragraph(vente.getNumero(), fontSousTitre);
        pNum.setAlignment(Element.ALIGN_RIGHT);
        cellFacture.addElement(pFacture);
        cellFacture.addElement(pNum);
        header.addCell(cellFacture);

        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // ---- Infos vente ----
        PdfPTable infos = new PdfPTable(2);
        infos.setWidthPercentage(100);
        infos.setSpacingBefore(10);

        ajouterInfoLigne(infos, "Date :",
            vente.getDateVente().format(FMT), fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Boutique :",
            vente.getSession().getLocal().getNom(), fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Client :",
            vente.getClient() != null
                ? vente.getClient().getNom() : "Client de passage",
            fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Téléphone :",
            vente.getClient() != null && vente.getClient().getTelephone() != null
                ? vente.getClient().getTelephone() : "-",
            fontLabel, fontValeur);

        doc.add(infos);
        doc.add(Chunk.NEWLINE);
        
        doc.add(infos);
        doc.add(Chunk.NEWLINE);

        // ✅ Mention retrait magasin si applicable
        if (vente.getMentionLivraison() != null
                && !vente.getMentionLivraison().isEmpty()) {
            doc.add(Chunk.NEWLINE);
            PdfPTable mentionTable = new PdfPTable(1);
            mentionTable.setWidthPercentage(100);
            PdfPCell mentionCell = new PdfPCell();
            mentionCell.setBackgroundColor(new BaseColor(227, 240, 255));
            mentionCell.setBorder(Rectangle.BOX);
            mentionCell.setBorderColor(new BaseColor(67, 97, 238));
            mentionCell.setPadding(12);
            Font fontMention = new Font(Font.FontFamily.HELVETICA, 10,
                Font.BOLD, new BaseColor(67, 97, 238));
            Paragraph pMention = new Paragraph(
                "📦 " + vente.getMentionLivraison(), fontMention);
            pMention.setAlignment(Element.ALIGN_CENTER);
            mentionCell.addElement(pMention);
            mentionTable.addCell(mentionCell);
            doc.add(mentionTable);
            doc.add(Chunk.NEWLINE);
        }


        // ---- Tableau articles ----
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1f, 1.2f, 1.5f});
        table.setSpacingBefore(10);

        String[] headers = {"Désignation", "Unité", "Qté", "Prix U.", "Total"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
            c.setBackgroundColor(DARK_BLUE);
            c.setPadding(8);
            c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }

        boolean pair = false;
        for (LigneVente l : lignes) {
            BaseColor bg = pair ? LIGHT_GREY : WHITE;
            ajouterLigneTableau(table, l.getArticle().getDesignation(), bg, fontCell, Element.ALIGN_LEFT);
            ajouterLigneTableau(table, l.getArticle().getUnite() != null ? l.getArticle().getUnite() : "-", bg, fontCell, Element.ALIGN_CENTER);
            ajouterLigneTableau(table, String.valueOf(l.getQuantite()), bg, fontCell, Element.ALIGN_CENTER);
            ajouterLigneTableau(table, formatMontant(l.getPrixUnitaire()) + " FCFA", bg, fontCell, Element.ALIGN_RIGHT);
            ajouterLigneTableau(table, formatMontant(l.getMontantLigne()) + " FCFA", bg, fontCell, Element.ALIGN_RIGHT);
            pair = !pair;
        }

        doc.add(table);

        // ---- Total ----
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(40);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.setSpacingBefore(10);

        PdfPCell labelTotal = new PdfPCell(new Phrase("TOTAL À PAYER", fontTotal));
        labelTotal.setBackgroundColor(DARK_BLUE);
        labelTotal.setPadding(10);
        labelTotal.setBorder(Rectangle.NO_BORDER);
        totalTable.addCell(labelTotal);

        PdfPCell valTotal = new PdfPCell(
            new Phrase(formatMontant(vente.getMontantTotal()) + " FCFA", fontTotal));
        valTotal.setBackgroundColor(GREEN);
        valTotal.setPadding(10);
        valTotal.setBorder(Rectangle.NO_BORDER);
        valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalTable.addCell(valTotal);

        doc.add(totalTable);

        // ---- Pied de page ----
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        Font fontPied = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC,
            new BaseColor(150, 150, 150));
        Paragraph pied = new Paragraph(
            "Merci pour votre confiance. — Document généré le "
            + java.time.LocalDateTime.now().format(FMT), fontPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(pied);

        doc.close();
        return out.toByteArray();
        
        
    }

    // =============================================
    // BORDEREAU D'ENCAISSEMENT
    // =============================================
    public byte[] genererBordereau(Long sessionId) throws Exception {
        SessionJournee session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session introuvable"));
        List<Vente> ventes = venteRepository.findBySessionId(sessionId);

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   WHITE);
        Font fontSub    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, WHITE);
        Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
        Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
        Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
        Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);

        // En-tête
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell cellH = new PdfPCell();
        cellH.setBackgroundColor(DARK_BLUE);
        cellH.setBorder(Rectangle.NO_BORDER);
        cellH.setPadding(15);
        Paragraph pH = new Paragraph("BORDEREAU D'ENCAISSEMENT\n", fontTitre);
        pH.add(new Phrase("Gestion Commerciale — " + session.getLocal().getNom(), fontSub));
        cellH.addElement(pH);
        header.addCell(cellH);
        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // Infos session
        PdfPTable infos = new PdfPTable(2);
        infos.setWidthPercentage(100);
        ajouterInfoLigne(infos, "Boutique :",
            session.getLocal().getNom(), fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Caissier :",
            session.getUtilisateur().getPrenom() + " "
            + session.getUtilisateur().getNom(), fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Ouverture :",
            session.getDateOuverture().format(FMT), fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Clôture :",
            session.getDateCloture() != null
                ? session.getDateCloture().format(FMT) : "En cours",
            fontLabel, fontValeur);
        ajouterInfoLigne(infos, "Fonds ouverture :",
            formatMontant(session.getMontantOuverture()) + " FCFA",
            fontLabel, fontValeur);
        doc.add(infos);
        doc.add(Chunk.NEWLINE);

        // Tableau ventes
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2.5f, 1.5f, 2f});
        table.setSpacingBefore(10);

        String[] cols = {"N° Vente", "Client", "Statut", "Montant"};
        for (String h : cols) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
            c.setBackgroundColor(DARK_BLUE);
            c.setPadding(8);
            c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }

        java.math.BigDecimal totalVentes = java.math.BigDecimal.ZERO;
        boolean pair = false;
        for (Vente v : ventes) {
            if (v.getStatut().equals("ANNULEE")) continue;
            BaseColor bg = pair ? LIGHT_GREY : WHITE;
            ajouterLigneTableau(table, v.getNumero(), bg, fontCell, Element.ALIGN_LEFT);
            ajouterLigneTableau(table,
                v.getClient() != null ? v.getClient().getNom() : "Passage",
                bg, fontCell, Element.ALIGN_LEFT);
            ajouterLigneTableau(table, v.getStatut(), bg, fontCell, Element.ALIGN_CENTER);
            ajouterLigneTableau(table,
                formatMontant(v.getMontantTotal()) + " FCFA",
                bg, fontCell, Element.ALIGN_RIGHT);
            totalVentes = totalVentes.add(v.getMontantTotal());
            pair = !pair;
        }
        doc.add(table);

        // Récapitulatif
        doc.add(Chunk.NEWLINE);
        PdfPTable recap = new PdfPTable(2);
        recap.setWidthPercentage(50);
        recap.setHorizontalAlignment(Element.ALIGN_RIGHT);
        recap.setSpacingBefore(5);

        ajouterRecapLigne(recap, "Fonds ouverture",
            formatMontant(session.getMontantOuverture()) + " FCFA",
            fontLabel, fontValeur);
        ajouterRecapLigne(recap, "Total ventes",
            formatMontant(totalVentes) + " FCFA", fontLabel, fontValeur);

        PdfPCell lTot = new PdfPCell(new Phrase("TOTAL CAISSE", fontTotal));
        lTot.setBackgroundColor(DARK_BLUE);
        lTot.setPadding(10); lTot.setBorder(Rectangle.NO_BORDER);
        recap.addCell(lTot);

        java.math.BigDecimal totalCaisse =
            session.getMontantOuverture().add(totalVentes);
        PdfPCell vTot = new PdfPCell(
            new Phrase(formatMontant(totalCaisse) + " FCFA", fontTotal));
        vTot.setBackgroundColor(GREEN);
        vTot.setPadding(10); vTot.setBorder(Rectangle.NO_BORDER);
        vTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        recap.addCell(vTot);

        doc.add(recap);

        // Signature
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        PdfPTable sig = new PdfPTable(2);
        sig.setWidthPercentage(100);
        Font fontSig = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, DARK_BLUE);

        PdfPCell sigCaissier = new PdfPCell();
        sigCaissier.setBorder(Rectangle.NO_BORDER);
        sigCaissier.addElement(new Paragraph("Signature du caissier :", fontSig));
        sigCaissier.addElement(new Paragraph("\n\n_______________________", fontSig));
        sig.addCell(sigCaissier);

        PdfPCell sigResp = new PdfPCell();
        sigResp.setBorder(Rectangle.NO_BORDER);
        sigResp.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sigResp.addElement(new Paragraph("Signature du responsable :", fontSig));
        sigResp.addElement(new Paragraph("\n\n_______________________", fontSig));
        sig.addCell(sigResp);

        doc.add(sig);
        doc.close();
        return out.toByteArray();
    }

 // =============================================
 // BON DE SORTIE SIGNÉ (avec zone vérification)
 // =============================================
 public byte[] genererBonSortie(Long bonId) throws Exception {
     BonSortie bon = bonSortieRepository.findById(bonId)
         .orElseThrow(() -> new RuntimeException("Bon de sortie introuvable"));

     Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
     ByteArrayOutputStream out = new ByteArrayOutputStream();
     PdfWriter.getInstance(doc, out);
     doc.open();

     Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   WHITE);
     Font fontSub    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, WHITE);
     Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
     Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
     Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

     // En-tête
     PdfPTable header = new PdfPTable(2);
     header.setWidthPercentage(100);
     header.setWidths(new float[]{2f, 1f});

     PdfPCell cellG = new PdfPCell();
     cellG.setBackgroundColor(DARK_BLUE);
     cellG.setBorder(Rectangle.NO_BORDER);
     cellG.setPadding(15);
     Paragraph pG = new Paragraph("BON DE SORTIE\n", fontTitre);
     pG.add(new Phrase("Approuvé — À vérifier avant sortie", fontSub));
     cellG.addElement(pG);
     header.addCell(cellG);

     PdfPCell cellD = new PdfPCell();
     cellD.setBackgroundColor(GREEN);
     cellD.setBorder(Rectangle.NO_BORDER);
     cellD.setPadding(15);
     Paragraph pD = new Paragraph(bon.getNumero(), fontTitre);
     pD.setAlignment(Element.ALIGN_RIGHT);
     cellD.addElement(pD);
     header.addCell(cellD);
     doc.add(header);
     doc.add(Chunk.NEWLINE);

     // Infos bon
     PdfPTable infos = new PdfPTable(2);
     infos.setWidthPercentage(100);
     infos.setSpacingBefore(10);

     ajouterInfoLigne(infos, "Numéro bon :",
         bon.getNumero(), fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Date approbation :",
         bon.getDateApprobation().format(FMT), fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Approuvé par :",
         bon.getResponsable().getPrenom() + " "
         + bon.getResponsable().getNom(), fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Article :",
         bon.getMouvement().getStock().getArticle().getDesignation(),
         fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Référence :",
         bon.getMouvement().getStock().getArticle().getReference(),
         fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Local de sortie :",
         bon.getMouvement().getStock().getLocal().getNom(),
         fontLabel, fontValeur);
     ajouterInfoLigne(infos, "Motif :",
         bon.getMouvement().getMotif(), fontLabel, fontValeur);
     if (bon.getObservation() != null && !bon.getObservation().isEmpty()) {
         ajouterInfoLigne(infos, "Observation responsable :",
             bon.getObservation(), fontLabel, fontValeur);
     }
     doc.add(infos);
     doc.add(Chunk.NEWLINE);

     // Quantité approuvée — mise en évidence
     PdfPTable qtApprouvee = new PdfPTable(1);
     qtApprouvee.setWidthPercentage(100);
     qtApprouvee.setSpacingBefore(5);

     PdfPCell cellQte = new PdfPCell();
     cellQte.setBackgroundColor(new BaseColor(212, 237, 218));
     cellQte.setBorder(Rectangle.BOX);
     cellQte.setBorderColor(new BaseColor(39, 174, 96));
     cellQte.setPadding(15);
     cellQte.setHorizontalAlignment(Element.ALIGN_CENTER);

     Font fontQteLabel = new Font(Font.FontFamily.HELVETICA, 11,
         Font.BOLD, new BaseColor(39, 174, 96));
     Font fontQteVal = new Font(Font.FontFamily.HELVETICA, 22,
         Font.BOLD, DARK_BLUE);

     Paragraph pQteLabel = new Paragraph(
         "QUANTITÉ AUTORISÉE À SORTIR", fontQteLabel);
     pQteLabel.setAlignment(Element.ALIGN_CENTER);

     Paragraph pQteVal = new Paragraph(
         bon.getMouvement().getQuantite() + " "
         + (bon.getMouvement().getStock().getArticle().getUnite() != null
             ? bon.getMouvement().getStock().getArticle().getUnite() : ""),
         fontQteVal);
     pQteVal.setAlignment(Element.ALIGN_CENTER);

     cellQte.addElement(pQteLabel);
     cellQte.addElement(pQteVal);
     qtApprouvee.addCell(cellQte);
     doc.add(qtApprouvee);
     doc.add(Chunk.NEWLINE);

     // Zone vérification magasinier
     PdfPTable titreZone = new PdfPTable(1);
     titreZone.setWidthPercentage(100);

     PdfPCell titreCell = new PdfPCell(
         new Phrase("ZONE DE VÉRIFICATION — À REMPLIR PAR LE MAGASINIER",
             new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
     titreCell.setBackgroundColor(DARK_BLUE);
     titreCell.setPadding(10);
     titreCell.setBorder(Rectangle.NO_BORDER);
     titreZone.addCell(titreCell);
     doc.add(titreZone);

     PdfPTable zoneVerif = new PdfPTable(2);
     zoneVerif.setWidthPercentage(100);
     zoneVerif.setWidths(new float[]{1f, 1f});

     PdfPCell caseQte = new PdfPCell();
     caseQte.setBorder(Rectangle.BOX);
     caseQte.setBorderColor(new BaseColor(200, 200, 200));
     caseQte.setPadding(15);
     Paragraph pQte = new Paragraph(
         "Quantité physique préparée :\n\n\n", fontLabel);
     pQte.add(new Phrase(
         "_______________  "
         + (bon.getMouvement().getStock().getArticle().getUnite() != null
             ? bon.getMouvement().getStock().getArticle().getUnite() : ""),
         fontValeur));
     caseQte.addElement(pQte);
     zoneVerif.addCell(caseQte);

     PdfPCell caseConf = new PdfPCell();
     caseConf.setBorder(Rectangle.BOX);
     caseConf.setBorderColor(new BaseColor(200, 200, 200));
     caseConf.setPadding(15);
     Paragraph pConf = new Paragraph("Vérification :\n\n", fontLabel);
     pConf.add(new Phrase("☐  Quantité conforme au bon\n\n", fontValeur));
     pConf.add(new Phrase("☐  Écart constaté — Voir observations", fontValeur));
     caseConf.addElement(pConf);
     zoneVerif.addCell(caseConf);

     doc.add(zoneVerif);

     // Zone observations
     PdfPTable zoneObs = new PdfPTable(1);
     zoneObs.setWidthPercentage(100);

     PdfPCell caseObs = new PdfPCell();
     caseObs.setBorder(Rectangle.BOX);
     caseObs.setBorderColor(new BaseColor(200, 200, 200));
     caseObs.setPadding(12);
     Paragraph pObs = new Paragraph(
         "Observations du magasinier :\n\n\n\n", fontLabel);
     caseObs.addElement(pObs);
     zoneObs.addCell(caseObs);
     doc.add(zoneObs);

     doc.add(Chunk.NEWLINE);
     doc.add(Chunk.NEWLINE);

     // Signatures
     PdfPTable sig = new PdfPTable(3);
     sig.setWidthPercentage(100);

     PdfPCell sigDem = new PdfPCell();
     sigDem.setBorder(Rectangle.NO_BORDER);
     sigDem.addElement(new Paragraph("Signature du demandeur :", fontSig));
     sigDem.addElement(new Paragraph(
         "\n\n_______________________", fontSig));
     sig.addCell(sigDem);

     PdfPCell sigMag = new PdfPCell();
     sigMag.setBorder(Rectangle.NO_BORDER);
     sigMag.setHorizontalAlignment(Element.ALIGN_CENTER);
     sigMag.addElement(new Paragraph("Signature du magasinier :", fontSig));
     sigMag.addElement(new Paragraph(
         "\n\n_______________________", fontSig));
     sig.addCell(sigMag);

     PdfPCell sigResp = new PdfPCell();
     sigResp.setBorder(Rectangle.NO_BORDER);
     sigResp.setHorizontalAlignment(Element.ALIGN_RIGHT);
     Font fontResponsable = new Font(Font.FontFamily.HELVETICA, 9,
         Font.BOLD, new BaseColor(39, 174, 96));
     sigResp.addElement(new Paragraph(
         "✓ Approuvé par :", fontResponsable));
     sigResp.addElement(new Paragraph(
         bon.getResponsable().getPrenom() + " "
         + bon.getResponsable().getNom(), fontSig));
     sigResp.addElement(new Paragraph(
         bon.getDateApprobation().format(FMT), fontSig));
     sigResp.addElement(new Paragraph(
         "\n_______________________", fontSig));
     sig.addCell(sigResp);

     doc.add(sig);

     // Pied de page
     doc.add(Chunk.NEWLINE);
     Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
         Font.ITALIC, new BaseColor(150, 150, 150));
     Paragraph pied = new Paragraph(
         "Document officiel — Gestion Commerciale — Généré le "
         + java.time.LocalDateTime.now().format(FMT), fontPied);
     pied.setAlignment(Element.ALIGN_CENTER);
     doc.add(pied);

     doc.close();
     return out.toByteArray();
 }

    // =============================================
    // MÉTHODES UTILITAIRES
    // =============================================
    private void ajouterInfoLigne(PdfPTable table,
                                   String label, String valeur,
                                   Font fontLabel, Font fontValeur) {
        PdfPCell cl = new PdfPCell(new Phrase(label, fontLabel));
        cl.setBorder(Rectangle.BOTTOM);
        cl.setBorderColor(new BaseColor(230, 230, 230));
        cl.setPadding(6);
        table.addCell(cl);

        PdfPCell cv = new PdfPCell(new Phrase(valeur, fontValeur));
        cv.setBorder(Rectangle.BOTTOM);
        cv.setBorderColor(new BaseColor(230, 230, 230));
        cv.setPadding(6);
        table.addCell(cv);
    }

    private void ajouterLigneTableau(PdfPTable table, String texte,
                                      BaseColor bg, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texte, font));
        c.setBackgroundColor(bg);
        c.setPadding(7);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        table.addCell(c);
    }

    private void ajouterRecapLigne(PdfPTable table,
                                    String label, String valeur,
                                    Font fontLabel, Font fontValeur) {
        PdfPCell cl = new PdfPCell(new Phrase(label, fontLabel));
        cl.setPadding(7); cl.setBorder(Rectangle.BOTTOM);
        cl.setBorderColor(new BaseColor(230, 230, 230));
        table.addCell(cl);

        PdfPCell cv = new PdfPCell(new Phrase(valeur, fontValeur));
        cv.setPadding(7); cv.setBorder(Rectangle.BOTTOM);
        cv.setBorderColor(new BaseColor(230, 230, 230));
        cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cv);
    }

    private String formatMontant(java.math.BigDecimal montant) {
        if (montant == null) return "0";
        return String.format("%,.0f", montant);
    }
 // =============================================
 // RAPPORT MENSUEL
 // =============================================
 public byte[] genererRapportMensuel(int annee, int mois) throws Exception {

     YearMonth yearMonth = YearMonth.of(annee, mois);
     LocalDateTime debut = yearMonth.atDay(1).atStartOfDay();
     LocalDateTime fin   = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();

     String nomMois = yearMonth.getMonth()
         .getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase();

     List<Vente> ventes = venteRepository
         .findVentesValideesBetween(debut, fin);

     // Calculs globaux
     java.math.BigDecimal totalMois = ventes.stream()
         .map(Vente::getMontantTotal)
         .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

     // Top articles
     List<Object[]> topArticles = ligneVenteRepository
         .findTopArticles(debut, fin);

     // Ventes par boutique
     java.util.Map<String, java.math.BigDecimal> parBoutique = new java.util.LinkedHashMap<>();
     java.util.Map<String, Integer> nbParBoutique = new java.util.LinkedHashMap<>();
     for (Vente v : ventes) {
         String boutique = v.getSession().getLocal().getNom();
         parBoutique.merge(boutique, v.getMontantTotal(),
             java.math.BigDecimal::add);
         nbParBoutique.merge(boutique, 1, Integer::sum);
     }

     // Ventes par jour
     java.util.Map<String, java.math.BigDecimal> parJour = new java.util.LinkedHashMap<>();
     DateTimeFormatter fmtJour = DateTimeFormatter.ofPattern("dd/MM");
     for (int j = 1; j <= yearMonth.lengthOfMonth(); j++) {
         LocalDateTime dJ = yearMonth.atDay(j).atStartOfDay();
         LocalDateTime fJ = dJ.plusDays(1);
         java.math.BigDecimal totalJour = ventes.stream()
             .filter(v -> v.getDateVente().isAfter(dJ)
                 && v.getDateVente().isBefore(fJ))
             .map(Vente::getMontantTotal)
             .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
         parJour.put(dJ.format(fmtJour), totalJour);
     }

     // Meilleur jour
     String meilleurJour = parJour.entrySet().stream()
         .max(java.util.Map.Entry.comparingByValue())
         .map(java.util.Map.Entry::getKey)
         .orElse("-");
     java.math.BigDecimal meilleurMontant = parJour.values().stream()
         .max(java.math.BigDecimal::compareTo)
         .orElse(java.math.BigDecimal.ZERO);

     // ---- Génération PDF ----
     Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
     ByteArrayOutputStream out = new ByteArrayOutputStream();
     PdfWriter.getInstance(doc, out);
     doc.open();

     Font fontTitre   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
     Font fontSub     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
     Font fontSection = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   DARK_BLUE);
     Font fontLabel   = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
     Font fontValeur  = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
     Font fontHeader  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
     Font fontCell    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
     Font fontTotal   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);
     Font fontKpi     = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   DARK_BLUE);
     Font fontKpiSub  = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL,
         new BaseColor(120, 120, 120));

     // En-tête
     PdfPTable header = new PdfPTable(2);
     header.setWidthPercentage(100);
     header.setWidths(new float[]{2f, 1f});

     PdfPCell cellG = new PdfPCell();
     cellG.setBackgroundColor(DARK_BLUE);
     cellG.setBorder(Rectangle.NO_BORDER);
     cellG.setPadding(15);
     Paragraph pG = new Paragraph("RAPPORT MENSUEL\n", fontTitre);
     pG.add(new Phrase("Gestion Commerciale — " + nomMois + " " + annee, fontSub));
     cellG.addElement(pG);
     header.addCell(cellG);

     PdfPCell cellD = new PdfPCell();
     cellD.setBackgroundColor(GREEN);
     cellD.setBorder(Rectangle.NO_BORDER);
     cellD.setPadding(15);
     Paragraph pD = new Paragraph(
         "Généré le\n" + java.time.LocalDate.now()
             .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSub);
     pD.setAlignment(Element.ALIGN_RIGHT);
     cellD.addElement(pD);
     header.addCell(cellD);
     doc.add(header);
     doc.add(Chunk.NEWLINE);

     // KPIs
     PdfPTable kpis = new PdfPTable(3);
     kpis.setWidthPercentage(100);
     kpis.setSpacingBefore(10);

     ajouterKpi(kpis, formatMontant(totalMois) + " FCFA",
         "Chiffre d'affaires du mois", DARK_BLUE, fontKpi, fontKpiSub);
     ajouterKpi(kpis, String.valueOf(ventes.size()),
         "Ventes validées", new BaseColor(39, 174, 96), fontKpi, fontKpiSub);
     ajouterKpi(kpis, meilleurJour + " (" + formatMontant(meilleurMontant) + " FCFA)",
         "Meilleur jour", new BaseColor(67, 97, 238), fontKpi, fontKpiSub);

     doc.add(kpis);
     doc.add(Chunk.NEWLINE);

     // Ventes par boutique
     Paragraph titBoutique = new Paragraph("Ventes par boutique", fontSection);
     titBoutique.setSpacingBefore(10);
     titBoutique.setSpacingAfter(6);
     doc.add(titBoutique);

     PdfPTable tableBoutique = new PdfPTable(3);
     tableBoutique.setWidthPercentage(100);
     tableBoutique.setWidths(new float[]{3f, 1.5f, 2f});

     for (String h : new String[]{"Boutique", "Nb ventes", "Total"}) {
         PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
         c.setBackgroundColor(DARK_BLUE);
         c.setPadding(8); c.setBorder(Rectangle.NO_BORDER);
         tableBoutique.addCell(c);
     }

     boolean pair = false;
     for (java.util.Map.Entry<String, java.math.BigDecimal> e
             : parBoutique.entrySet()) {
         BaseColor bg = pair ? LIGHT_GREY : WHITE;
         ajouterLigneTableau(tableBoutique, e.getKey(),
             bg, fontCell, Element.ALIGN_LEFT);
         ajouterLigneTableau(tableBoutique,
             String.valueOf(nbParBoutique.get(e.getKey())),
             bg, fontCell, Element.ALIGN_CENTER);
         ajouterLigneTableau(tableBoutique,
             formatMontant(e.getValue()) + " FCFA",
             bg, fontCell, Element.ALIGN_RIGHT);
         pair = !pair;
     }

     // Ligne total
     PdfPCell totLabel = new PdfPCell(new Phrase("TOTAL", fontTotal));
     totLabel.setBackgroundColor(DARK_BLUE);
     totLabel.setPadding(8); totLabel.setBorder(Rectangle.NO_BORDER);
     totLabel.setColspan(2);
     tableBoutique.addCell(totLabel);

     PdfPCell totVal = new PdfPCell(
         new Phrase(formatMontant(totalMois) + " FCFA", fontTotal));
     totVal.setBackgroundColor(GREEN);
     totVal.setPadding(8); totVal.setBorder(Rectangle.NO_BORDER);
     totVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
     tableBoutique.addCell(totVal);

     doc.add(tableBoutique);
     doc.add(Chunk.NEWLINE);

     // Top 5 articles
     Paragraph titArticles = new Paragraph("Top articles vendus", fontSection);
     titArticles.setSpacingBefore(10);
     titArticles.setSpacingAfter(6);
     doc.add(titArticles);

     PdfPTable tableArticles = new PdfPTable(3);
     tableArticles.setWidthPercentage(100);
     tableArticles.setWidths(new float[]{3f, 1.5f, 2f});

     for (String h : new String[]{"Article", "Qté vendue", "Montant total"}) {
         PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
         c.setBackgroundColor(DARK_BLUE);
         c.setPadding(8); c.setBorder(Rectangle.NO_BORDER);
         tableArticles.addCell(c);
     }

     pair = false;
     int rang = 0;
     for (Object[] row : topArticles) {
         if (rang++ >= 5) break;
         BaseColor bg = pair ? LIGHT_GREY : WHITE;
         ajouterLigneTableau(tableArticles,
             row[0] != null ? row[0].toString() : "-",
             bg, fontCell, Element.ALIGN_LEFT);
         ajouterLigneTableau(tableArticles,
             row[1] != null ? row[1].toString() : "0",
             bg, fontCell, Element.ALIGN_CENTER);
         ajouterLigneTableau(tableArticles,
             row[2] != null ? formatMontant(
                 new java.math.BigDecimal(row[2].toString())) + " FCFA" : "0 FCFA",
             bg, fontCell, Element.ALIGN_RIGHT);
         pair = !pair;
     }

     if (topArticles.isEmpty()) {
         PdfPCell vide = new PdfPCell(
             new Phrase("Aucune vente ce mois", fontCell));
         vide.setColspan(3);
         vide.setPadding(10);
         vide.setHorizontalAlignment(Element.ALIGN_CENTER);
         tableArticles.addCell(vide);
     }

     doc.add(tableArticles);
     doc.add(Chunk.NEWLINE);

     // Évolution journalière
     Paragraph titJours = new Paragraph(
         "Évolution journalière des ventes", fontSection);
     titJours.setSpacingBefore(10);
     titJours.setSpacingAfter(6);
     doc.add(titJours);

     PdfPTable tableJours = new PdfPTable(3);
     tableJours.setWidthPercentage(100);
     tableJours.setWidths(new float[]{2f, 1.5f, 2f});

     for (String h : new String[]{"Date", "Nb ventes", "Montant"}) {
         PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
         c.setBackgroundColor(DARK_BLUE);
         c.setPadding(8); c.setBorder(Rectangle.NO_BORDER);
         tableJours.addCell(c);
     }

     pair = false;
     DateTimeFormatter fmtJourFull =
         DateTimeFormatter.ofPattern("dd/MM");
     for (java.util.Map.Entry<String, java.math.BigDecimal> e
             : parJour.entrySet()) {
         if (e.getValue().compareTo(java.math.BigDecimal.ZERO) == 0
             && ventes.isEmpty()) continue;
         BaseColor bg = pair ? LIGHT_GREY : WHITE;
         // Mettre en vert le meilleur jour
         if (e.getKey().equals(meilleurJour)
             && meilleurMontant.compareTo(java.math.BigDecimal.ZERO) > 0) {
             bg = new BaseColor(212, 237, 218);
         }
         long nbJour = ventes.stream()
             .filter(v -> v.getDateVente().format(fmtJourFull).equals(e.getKey()))
             .count();
         ajouterLigneTableau(tableJours, e.getKey(),
             bg, fontCell, Element.ALIGN_LEFT);
         ajouterLigneTableau(tableJours, String.valueOf(nbJour),
             bg, fontCell, Element.ALIGN_CENTER);
         ajouterLigneTableau(tableJours,
             formatMontant(e.getValue()) + " FCFA",
             bg, fontCell, Element.ALIGN_RIGHT);
         pair = !pair;
     }

     doc.add(tableJours);

     // Pied de page
     doc.add(Chunk.NEWLINE);
     Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
         Font.ITALIC, new BaseColor(150, 150, 150));
     Paragraph pied = new Paragraph(
         "Rapport généré automatiquement — Gestion Commerciale — "
         + java.time.LocalDateTime.now().format(FMT), fontPied);
     pied.setAlignment(Element.ALIGN_CENTER);
     doc.add(pied);

     doc.close();
     return out.toByteArray();
 }

 // Méthode utilitaire KPI
 private void ajouterKpi(PdfPTable table, String valeur, String label,
                          BaseColor couleur, Font fontVal, Font fontLab) {
     PdfPCell cell = new PdfPCell();
     cell.setBorder(Rectangle.BOX);
     cell.setBorderColor(new BaseColor(230, 230, 230));
     cell.setPadding(12);
     cell.setHorizontalAlignment(Element.ALIGN_CENTER);

     Paragraph pVal = new Paragraph(valeur, fontVal);
     pVal.setAlignment(Element.ALIGN_CENTER);
     Paragraph pLab = new Paragraph(label, fontLab);
     pLab.setAlignment(Element.ALIGN_CENTER);

     cell.addElement(pVal);
     cell.addElement(pLab);
     table.addCell(cell);
 }
//=============================================
//BON DE RÉCEPTION
//=============================================
public byte[] genererBonReception(Long stockId, Integer quantiteRecue) throws Exception {

  com.gestion.commercial.entity.Stock stock =
      stockRepository.findById(stockId)
          .orElseThrow(() -> new RuntimeException("Stock introuvable"));

  Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  PdfWriter writer = PdfWriter.getInstance(doc, out);
  doc.open();

  Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
  Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
  Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
  Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
  Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
  Font fontAlert  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,
      new BaseColor(192, 57, 43));

  // Numéro bon réception
  String numeroBon = String.format("BR-%s-%04d",
      java.time.LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd")),
      stockId);

  // En-tête
  PdfPTable header = new PdfPTable(2);
  header.setWidthPercentage(100);
  header.setWidths(new float[]{2f, 1f});

  PdfPCell cellG = new PdfPCell();
  cellG.setBackgroundColor(DARK_BLUE);
  cellG.setBorder(Rectangle.NO_BORDER);
  cellG.setPadding(15);
  Paragraph pG = new Paragraph("BON DE RÉCEPTION\n", fontTitre);
  pG.add(new Phrase("Gestion Commerciale — Contrôle à la réception", fontSub));
  cellG.addElement(pG);
  header.addCell(cellG);

  PdfPCell cellD = new PdfPCell();
  cellD.setBackgroundColor(GREEN);
  cellD.setBorder(Rectangle.NO_BORDER);
  cellD.setPadding(15);
  Paragraph pD = new Paragraph(numeroBon, fontTitre);
  pD.setAlignment(Element.ALIGN_RIGHT);
  cellD.addElement(pD);
  header.addCell(cellD);
  doc.add(header);
  doc.add(Chunk.NEWLINE);

  // Infos réception
  PdfPTable infos = new PdfPTable(2);
  infos.setWidthPercentage(100);
  infos.setSpacingBefore(10);

  ajouterInfoLigne(infos, "Date de réception :",
      java.time.LocalDateTime.now().format(FMT), fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Magasin de destination :",
      stock.getLocal().getNom() + " — " + stock.getLocal().getAdresse(),
      fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Article :",
      stock.getArticle().getDesignation(), fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Référence article :",
      stock.getArticle().getReference(), fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Unité de mesure :",
      stock.getArticle().getUnite() != null
          ? stock.getArticle().getUnite() : "-",
      fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Quantité enregistrée dans le système :",
      quantiteRecue + " " + (stock.getArticle().getUnite() != null
          ? stock.getArticle().getUnite() : ""),
      fontLabel, fontValeur);
  ajouterInfoLigne(infos, "Stock total après réception :",
      stock.getQuantite() + " " + (stock.getArticle().getUnite() != null
          ? stock.getArticle().getUnite() : ""),
      fontLabel, fontValeur);

  doc.add(infos);
  doc.add(Chunk.NEWLINE);

  // Zone de vérification magasinier
  PdfPTable verification = new PdfPTable(1);
  verification.setWidthPercentage(100);
  verification.setSpacingBefore(10);

  PdfPCell titreVerif = new PdfPCell(
      new Phrase("ZONE DE VÉRIFICATION — À REMPLIR PAR LE MAGASINIER",
          new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
  titreVerif.setBackgroundColor(DARK_BLUE);
  titreVerif.setPadding(10);
  titreVerif.setBorder(Rectangle.NO_BORDER);
  verification.addCell(titreVerif);
  doc.add(verification);

  PdfPTable zoneVerif = new PdfPTable(2);
  zoneVerif.setWidthPercentage(100);
  zoneVerif.setWidths(new float[]{1f, 1f});

  // Case quantité physique
  PdfPCell caseQte = new PdfPCell();
  caseQte.setBorder(Rectangle.BOX);
  caseQte.setBorderColor(new BaseColor(200, 200, 200));
  caseQte.setPadding(15);
  Paragraph pQte = new Paragraph(
      "Quantité physique comptée :\n\n\n", fontLabel);
  pQte.add(new Phrase(
      "_______________  " + (stock.getArticle().getUnite() != null
          ? stock.getArticle().getUnite() : ""),
      fontValeur));
  caseQte.addElement(pQte);
  zoneVerif.addCell(caseQte);

  // Case conformité
  PdfPCell caseConf = new PdfPCell();
  caseConf.setBorder(Rectangle.BOX);
  caseConf.setBorderColor(new BaseColor(200, 200, 200));
  caseConf.setPadding(15);
  Paragraph pConf = new Paragraph("Conformité :\n\n", fontLabel);
  pConf.add(new Phrase("☐  Conforme\n\n", fontValeur));
  pConf.add(new Phrase("☐  Non conforme — Écart constaté", fontValeur));
  caseConf.addElement(pConf);
  zoneVerif.addCell(caseConf);

  doc.add(zoneVerif);

  // Zone observations
  PdfPTable zoneObs = new PdfPTable(1);
  zoneObs.setWidthPercentage(100);

  PdfPCell caseObs = new PdfPCell();
  caseObs.setBorder(Rectangle.BOX);
  caseObs.setBorderColor(new BaseColor(200, 200, 200));
  caseObs.setPadding(12);
  Paragraph pObs = new Paragraph(
      "Observations / Réserves :\n\n\n\n\n", fontLabel);
  caseObs.addElement(pObs);
  zoneObs.addCell(caseObs);
  doc.add(zoneObs);

  doc.add(Chunk.NEWLINE);
  doc.add(Chunk.NEWLINE);

  // Signatures
  PdfPTable sig = new PdfPTable(3);
  sig.setWidthPercentage(100);

  PdfPCell sigMag = new PdfPCell();
  sigMag.setBorder(Rectangle.NO_BORDER);
  sigMag.addElement(new Paragraph("Signature du magasinier :", fontSig));
  sigMag.addElement(new Paragraph(
      "\n\n_______________________", fontSig));
  sig.addCell(sigMag);

  PdfPCell sigResp = new PdfPCell();
  sigResp.setBorder(Rectangle.NO_BORDER);
  sigResp.setHorizontalAlignment(Element.ALIGN_CENTER);
  sigResp.addElement(new Paragraph("Cachet de l'entreprise :", fontSig));
  sigResp.addElement(new Paragraph("\n\n", fontSig));
  sig.addCell(sigResp);

  PdfPCell sigDir = new PdfPCell();
  sigDir.setBorder(Rectangle.NO_BORDER);
  sigDir.setHorizontalAlignment(Element.ALIGN_RIGHT);
  sigDir.addElement(new Paragraph("Signature du responsable :", fontSig));
  sigDir.addElement(new Paragraph(
      "\n\n_______________________", fontSig));
  sig.addCell(sigDir);

  doc.add(sig);

  // Pied de page
  doc.add(Chunk.NEWLINE);
  Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
      Font.ITALIC, new BaseColor(150, 150, 150));
  Paragraph pied = new Paragraph(
      "Document généré le " + java.time.LocalDateTime.now().format(FMT)
      + " — Gestion Commerciale", fontPied);
  pied.setAlignment(Element.ALIGN_CENTER);
  doc.add(pied);

  doc.close();
  return out.toByteArray();
}
public byte[] genererBonReceptionMultiple(List<Long> stockIds,
        List<Integer> quantites)
throws Exception {

Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
ByteArrayOutputStream out = new ByteArrayOutputStream();
PdfWriter.getInstance(doc, out);
doc.open();

Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);
Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

String numeroBon = String.format("BR-%s-%04d",
java.time.LocalDateTime.now()
.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
stockIds.get(0));

// En-tête
PdfPTable header = new PdfPTable(2);
header.setWidthPercentage(100);
header.setWidths(new float[]{2f, 1f});

PdfPCell cellG = new PdfPCell();
cellG.setBackgroundColor(DARK_BLUE);
cellG.setBorder(Rectangle.NO_BORDER);
cellG.setPadding(15);
Paragraph pG = new Paragraph("BON DE RÉCEPTION\n", fontTitre);
pG.add(new Phrase("Gestion Commerciale — Contrôle à la réception", fontSub));
cellG.addElement(pG);
header.addCell(cellG);

PdfPCell cellD = new PdfPCell();
cellD.setBackgroundColor(GREEN);
cellD.setBorder(Rectangle.NO_BORDER);
cellD.setPadding(15);
Paragraph pD = new Paragraph(numeroBon, fontTitre);
pD.setAlignment(Element.ALIGN_RIGHT);
Paragraph pDate = new Paragraph(
java.time.LocalDateTime.now().format(FMT), fontSub);
pDate.setAlignment(Element.ALIGN_RIGHT);
cellD.addElement(pD);
cellD.addElement(pDate);
header.addCell(cellD);
doc.add(header);
doc.add(Chunk.NEWLINE);

// Infos générales
PdfPTable infos = new PdfPTable(2);
infos.setWidthPercentage(100);
ajouterInfoLigne(infos, "Date de réception :",
java.time.LocalDateTime.now().format(FMT), fontLabel, fontValeur);
ajouterInfoLigne(infos, "Nombre d'articles :",
String.valueOf(stockIds.size()), fontLabel, fontValeur);
doc.add(infos);
doc.add(Chunk.NEWLINE);

// Tableau des articles reçus
PdfPTable table = new PdfPTable(5);
table.setWidthPercentage(100);
table.setWidths(new float[]{2.5f, 1f, 2f, 1.5f, 1.5f});
table.setSpacingBefore(10);

for (String h : new String[]{
"Article", "Référence", "Local", "Qté reçue", "Stock total"}) {
PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
c.setBackgroundColor(DARK_BLUE);
c.setPadding(8);
c.setBorder(Rectangle.NO_BORDER);
table.addCell(c);
}

boolean pair = false;
int totalArticles = 0;
for (int i = 0; i < stockIds.size(); i++) {
com.gestion.commercial.entity.Stock stock =
stockRepository.findById(stockIds.get(i)).orElse(null);
if (stock == null) continue;

BaseColor bg = pair ? LIGHT_GREY : WHITE;
int qteRecue = quantites.get(i);
totalArticles += qteRecue;

ajouterLigneTableau(table,
stock.getArticle().getDesignation(), bg, fontCell, Element.ALIGN_LEFT);
ajouterLigneTableau(table,
stock.getArticle().getReference(), bg, fontCell, Element.ALIGN_CENTER);
ajouterLigneTableau(table,
stock.getLocal().getNom(), bg, fontCell, Element.ALIGN_LEFT);
ajouterLigneTableau(table,
qteRecue + " " + (stock.getArticle().getUnite() != null
? stock.getArticle().getUnite() : ""),
bg, fontCell, Element.ALIGN_CENTER);
ajouterLigneTableau(table,
stock.getQuantite() + " " + (stock.getArticle().getUnite() != null
? stock.getArticle().getUnite() : ""),
bg, fontCell, Element.ALIGN_CENTER);
pair = !pair;
}



// Ligne total
PdfPCell totLabel = new PdfPCell(
new Phrase("TOTAL ARTICLES REÇUS", fontTotal));
totLabel.setBackgroundColor(DARK_BLUE);
totLabel.setPadding(8);
totLabel.setBorder(Rectangle.NO_BORDER);
totLabel.setColspan(3);
table.addCell(totLabel);

PdfPCell totVal = new PdfPCell(
new Phrase(String.valueOf(totalArticles), fontTotal));
totVal.setBackgroundColor(GREEN);
totVal.setPadding(8);
totVal.setBorder(Rectangle.NO_BORDER);
totVal.setColspan(2);
totVal.setHorizontalAlignment(Element.ALIGN_CENTER);
table.addCell(totVal);

doc.add(table);
doc.add(Chunk.NEWLINE);

// Zone vérification
PdfPTable titreVerif = new PdfPTable(1);
titreVerif.setWidthPercentage(100);
PdfPCell titreCell = new PdfPCell(
new Phrase("ZONE DE VÉRIFICATION — À REMPLIR PAR LE MAGASINIER",
new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
titreCell.setBackgroundColor(DARK_BLUE);
titreCell.setPadding(10);
titreCell.setBorder(Rectangle.NO_BORDER);
titreVerif.addCell(titreCell);
doc.add(titreVerif);

PdfPTable zoneVerif = new PdfPTable(2);
zoneVerif.setWidthPercentage(100);
zoneVerif.setWidths(new float[]{1f, 1f});

PdfPCell caseConf = new PdfPCell();
caseConf.setBorder(Rectangle.BOX);
caseConf.setBorderColor(new BaseColor(200, 200, 200));
caseConf.setPadding(15);
Paragraph pConf = new Paragraph("Conformité globale :\n\n", fontLabel);
pConf.add(new Phrase("☐  Tous les articles conformes\n\n", fontValeur));
pConf.add(new Phrase("☐  Écart(s) constaté(s)", fontValeur));
caseConf.addElement(pConf);
zoneVerif.addCell(caseConf);

PdfPCell caseObs = new PdfPCell();
caseObs.setBorder(Rectangle.BOX);
caseObs.setBorderColor(new BaseColor(200, 200, 200));
caseObs.setPadding(15);
Paragraph pObs = new Paragraph(
"Observations / Réserves :\n\n\n\n", fontLabel);
caseObs.addElement(pObs);
zoneVerif.addCell(caseObs);

doc.add(zoneVerif);
doc.add(Chunk.NEWLINE);
doc.add(Chunk.NEWLINE);

// Signatures
PdfPTable sig = new PdfPTable(3);
sig.setWidthPercentage(100);

PdfPCell sigMag = new PdfPCell();
sigMag.setBorder(Rectangle.NO_BORDER);
sigMag.addElement(new Paragraph("Signature du magasinier :", fontSig));
sigMag.addElement(new Paragraph("\n\n_______________________", fontSig));
sig.addCell(sigMag);

PdfPCell sigCachet = new PdfPCell();
sigCachet.setBorder(Rectangle.NO_BORDER);
sigCachet.setHorizontalAlignment(Element.ALIGN_CENTER);
sigCachet.addElement(new Paragraph("Cachet de l'entreprise :", fontSig));
sigCachet.addElement(new Paragraph("\n\n", fontSig));
sig.addCell(sigCachet);

PdfPCell sigResp = new PdfPCell();
sigResp.setBorder(Rectangle.NO_BORDER);
sigResp.setHorizontalAlignment(Element.ALIGN_RIGHT);
sigResp.addElement(new Paragraph("Signature du responsable :", fontSig));
sigResp.addElement(new Paragraph("\n\n_______________________", fontSig));
sig.addCell(sigResp);

doc.add(sig);

// Pied de page
doc.add(Chunk.NEWLINE);
Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
Font.ITALIC, new BaseColor(150, 150, 150));
Paragraph pied = new Paragraph(
"Document généré le " + java.time.LocalDateTime.now().format(FMT)
+ " — Gestion Commerciale", fontPied);
pied.setAlignment(Element.ALIGN_CENTER);
doc.add(pied);

doc.close();
return out.toByteArray();
}
//=============================================
//BON DE RÉCEPTION GROUPÉ
//=============================================
public byte[] genererBonReceptionGroupe(Long receptionId) throws Exception {

 Reception reception = receptionRepository.findById(receptionId)
     .orElseThrow(() -> new RuntimeException("Réception introuvable"));
 List<ReceptionLigne> lignes = reception.getLignes();

 String numeroBon = String.format("BR-%s-%04d",
     reception.getDateReception()
         .format(DateTimeFormatter.ofPattern("yyyyMMdd")),
     receptionId);

 Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
 ByteArrayOutputStream out = new ByteArrayOutputStream();
 PdfWriter.getInstance(doc, out);
 doc.open();

 Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
 Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
 Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
 Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
 Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);
 Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

 // En-tête
 PdfPTable header = new PdfPTable(2);
 header.setWidthPercentage(100);
 header.setWidths(new float[]{2f, 1f});

 PdfPCell cellG = new PdfPCell();
 cellG.setBackgroundColor(DARK_BLUE);
 cellG.setBorder(Rectangle.NO_BORDER);
 cellG.setPadding(15);
 Paragraph pG = new Paragraph("BON DE RÉCEPTION\n", fontTitre);
 pG.add(new Phrase("Gestion Commerciale — Contrôle à la réception", fontSub));
 cellG.addElement(pG);
 header.addCell(cellG);

 PdfPCell cellD = new PdfPCell();
 cellD.setBackgroundColor(GREEN);
 cellD.setBorder(Rectangle.NO_BORDER);
 cellD.setPadding(15);
 Paragraph pD = new Paragraph(numeroBon, fontTitre);
 pD.setAlignment(Element.ALIGN_RIGHT);
 cellD.addElement(pD);
 header.addCell(cellD);
 doc.add(header);
 doc.add(Chunk.NEWLINE);

 // Infos réception
 PdfPTable infos = new PdfPTable(2);
 infos.setWidthPercentage(100);
 infos.setSpacingBefore(10);

 ajouterInfoLigne(infos, "Date de réception :",
     reception.getDateReception().format(FMT), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Magasin de destination :",
     reception.getLocal().getNom()
     + " — " + reception.getLocal().getAdresse(),
     fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Réceptionné par :",
     reception.getUtilisateur().getPrenom()
     + " " + reception.getUtilisateur().getNom(),
     fontLabel, fontValeur);
 if (reception.getFournisseur() != null
         && !reception.getFournisseur().isEmpty()) {
     ajouterInfoLigne(infos, "Fournisseur :",
         reception.getFournisseur(), fontLabel, fontValeur);
 }
 if (reception.getReference() != null
         && !reception.getReference().isEmpty()) {
     ajouterInfoLigne(infos, "Référence commande :",
         reception.getReference(), fontLabel, fontValeur);
 }
 ajouterInfoLigne(infos, "Nombre d'articles :",
     lignes.size() + " référence(s)", fontLabel, fontValeur);
 doc.add(infos);
 doc.add(Chunk.NEWLINE);

 // Tableau articles reçus
 PdfPTable table = new PdfPTable(5);
 table.setWidthPercentage(100);
 table.setWidths(new float[]{1.2f, 3f, 1f, 1.5f, 1.5f});
 table.setSpacingBefore(10);

 for (String h : new String[]{
         "Référence", "Désignation", "Unité",
         "Qté reçue", "Prix achat"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(DARK_BLUE);
     c.setPadding(8);
     c.setBorder(Rectangle.NO_BORDER);
     table.addCell(c);
 }

 boolean pair = false;
 for (ReceptionLigne ligne : lignes) {
     BaseColor bg = pair ? LIGHT_GREY : WHITE;
     ajouterLigneTableau(table,
         ligne.getArticle().getReference(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         ligne.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         ligne.getArticle().getUnite() != null
             ? ligne.getArticle().getUnite() : "-",
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         String.valueOf(ligne.getQuantite()),
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         ligne.getPrixAchat() != null
             ? formatMontant(ligne.getPrixAchat()) + " FCFA" : "-",
         bg, fontCell, Element.ALIGN_RIGHT);
     pair = !pair;
 }
 doc.add(table);
 doc.add(Chunk.NEWLINE);

 // Zone vérification magasinier
 PdfPTable titreZone = new PdfPTable(1);
 titreZone.setWidthPercentage(100);
 PdfPCell titreCell = new PdfPCell(
     new Phrase("ZONE DE VÉRIFICATION — À REMPLIR PAR LE MAGASINIER",
         new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
 titreCell.setBackgroundColor(DARK_BLUE);
 titreCell.setPadding(10);
 titreCell.setBorder(Rectangle.NO_BORDER);
 titreZone.addCell(titreCell);
 doc.add(titreZone);

 // Tableau de vérification avec colonnes à remplir
 PdfPTable tableVerif = new PdfPTable(4);
 tableVerif.setWidthPercentage(100);
 tableVerif.setWidths(new float[]{3f, 1.5f, 1.5f, 2f});

 for (String h : new String[]{
         "Article", "Qté attendue",
         "Qté comptée", "Conformité"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(new BaseColor(67, 97, 238));
     c.setPadding(8);
     c.setBorder(Rectangle.NO_BORDER);
     tableVerif.addCell(c);
 }

 pair = false;
 for (ReceptionLigne ligne : lignes) {
     BaseColor bg = pair ? LIGHT_GREY : WHITE;
     ajouterLigneTableau(tableVerif,
         ligne.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(tableVerif,
         ligne.getQuantite() + " "
         + (ligne.getArticle().getUnite() != null
             ? ligne.getArticle().getUnite() : ""),
         bg, fontCell, Element.ALIGN_CENTER);
     // Colonne à remplir manuellement
     ajouterLigneTableau(tableVerif, "___________",
         bg, fontCell, Element.ALIGN_CENTER);
     // Case conformité
     PdfPCell confCell = new PdfPCell();
     confCell.setBackgroundColor(bg);
     confCell.setPadding(7);
     confCell.setBorder(Rectangle.NO_BORDER);
     Paragraph pConf = new Paragraph(
         "☐ OK  ☐ Écart", fontCell);
     confCell.addElement(pConf);
     tableVerif.addCell(confCell);
     pair = !pair;
 }
 doc.add(tableVerif);

 // Zone observations
 doc.add(Chunk.NEWLINE);
 PdfPTable zoneObs = new PdfPTable(1);
 zoneObs.setWidthPercentage(100);
 PdfPCell caseObs = new PdfPCell();
 caseObs.setBorder(Rectangle.BOX);
 caseObs.setBorderColor(new BaseColor(200, 200, 200));
 caseObs.setPadding(12);
 caseObs.addElement(new Paragraph(
     "Observations générales :\n\n\n\n", fontLabel));
 zoneObs.addCell(caseObs);
 doc.add(zoneObs);

 // Signatures
 doc.add(Chunk.NEWLINE);
 doc.add(Chunk.NEWLINE);
 PdfPTable sig = new PdfPTable(3);
 sig.setWidthPercentage(100);

 PdfPCell s1 = new PdfPCell();
 s1.setBorder(Rectangle.NO_BORDER);
 s1.addElement(new Paragraph("Signature du magasinier :", fontSig));
 s1.addElement(new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(s1);

 PdfPCell s2 = new PdfPCell();
 s2.setBorder(Rectangle.NO_BORDER);
 s2.setHorizontalAlignment(Element.ALIGN_CENTER);
 s2.addElement(new Paragraph("Cachet de l'entreprise :", fontSig));
 s2.addElement(new Paragraph("\n\n", fontSig));
 sig.addCell(s2);

 PdfPCell s3 = new PdfPCell();
 s3.setBorder(Rectangle.NO_BORDER);
 s3.setHorizontalAlignment(Element.ALIGN_RIGHT);
 s3.addElement(new Paragraph("Signature du responsable :", fontSig));
 s3.addElement(new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(s3);
 doc.add(sig);

 // Pied de page
 doc.add(Chunk.NEWLINE);
 Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
     Font.ITALIC, new BaseColor(150, 150, 150));
 Paragraph pied = new Paragraph(
     "Document généré le "
     + java.time.LocalDateTime.now().format(FMT)
     + " — Gestion Commerciale", fontPied);
 pied.setAlignment(Element.ALIGN_CENTER);
 doc.add(pied);

 doc.close();
 return out.toByteArray();
}

//=============================================
//BON DE RÉCEPTION DEPUIS SESSION (multi-articles)
//=============================================
public byte[] genererBonReceptionDepuisSession(
     List<Long> stockIds,
     List<Integer> quantites,
     List<java.util.Map<String, Object>> panier) throws Exception {

 String numeroBon = String.format("BR-%s-%04d",
     java.time.LocalDateTime.now()
         .format(DateTimeFormatter.ofPattern("yyyyMMdd")),
     stockIds.get(0));

 Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
 ByteArrayOutputStream out = new ByteArrayOutputStream();
 PdfWriter.getInstance(doc, out);
 doc.open();

 Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
 Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
 Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
 Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
 Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

 // En-tête
 PdfPTable header = new PdfPTable(2);
 header.setWidthPercentage(100);
 header.setWidths(new float[]{2f, 1f});

 PdfPCell cellG = new PdfPCell();
 cellG.setBackgroundColor(DARK_BLUE);
 cellG.setBorder(Rectangle.NO_BORDER);
 cellG.setPadding(15);
 Paragraph pG = new Paragraph("BON DE RÉCEPTION\n", fontTitre);
 pG.add(new Phrase(
     "Gestion Commerciale — Contrôle à la réception", fontSub));
 cellG.addElement(pG);
 header.addCell(cellG);

 PdfPCell cellD = new PdfPCell();
 cellD.setBackgroundColor(GREEN);
 cellD.setBorder(Rectangle.NO_BORDER);
 cellD.setPadding(15);
 Paragraph pD = new Paragraph(numeroBon, fontTitre);
 pD.setAlignment(Element.ALIGN_RIGHT);
 cellD.addElement(pD);
 header.addCell(cellD);
 doc.add(header);
 doc.add(Chunk.NEWLINE);

 // Infos générales
 PdfPTable infos = new PdfPTable(2);
 infos.setWidthPercentage(100);
 infos.setSpacingBefore(10);
 ajouterInfoLigne(infos, "Date de réception :",
     java.time.LocalDateTime.now().format(FMT), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Nombre d'articles :",
     stockIds.size() + " référence(s)", fontLabel, fontValeur);
 doc.add(infos);
 doc.add(Chunk.NEWLINE);

 // Tableau articles reçus
 PdfPTable table = new PdfPTable(5);
 table.setWidthPercentage(100);
 table.setWidths(new float[]{1.2f, 3f, 2f, 1f, 1f});
 table.setSpacingBefore(10);

 for (String h : new String[]{
         "Référence", "Désignation", "Local", "Qté reçue", "Unité"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(DARK_BLUE);
     c.setPadding(8);
     c.setBorder(Rectangle.NO_BORDER);
     table.addCell(c);
 }

 boolean pair = false;
 for (int i = 0; i < stockIds.size(); i++) {
     Stock stock = stockRepository.findById(stockIds.get(i))
         .orElse(null);
     if (stock == null) continue;

     BaseColor bg = pair ? LIGHT_GREY : WHITE;
     ajouterLigneTableau(table,
         stock.getArticle().getReference(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         stock.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         stock.getLocal().getNom(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         String.valueOf(quantites.get(i)),
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         stock.getArticle().getUnite() != null
             ? stock.getArticle().getUnite() : "-",
         bg, fontCell, Element.ALIGN_CENTER);
     pair = !pair;
 }
 doc.add(table);
 doc.add(Chunk.NEWLINE);

 // Zone vérification
 PdfPTable titreZone = new PdfPTable(1);
 titreZone.setWidthPercentage(100);
 PdfPCell titreCell = new PdfPCell(new Phrase(
     "ZONE DE VÉRIFICATION — À REMPLIR PAR LE MAGASINIER",
     new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE)));
 titreCell.setBackgroundColor(DARK_BLUE);
 titreCell.setPadding(10);
 titreCell.setBorder(Rectangle.NO_BORDER);
 titreZone.addCell(titreCell);
 doc.add(titreZone);

 PdfPTable tableVerif = new PdfPTable(4);
 tableVerif.setWidthPercentage(100);
 tableVerif.setWidths(new float[]{3f, 1.5f, 1.5f, 2f});

 for (String h : new String[]{
         "Article", "Qté attendue", "Qté comptée", "Conformité"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(new BaseColor(67, 97, 238));
     c.setPadding(8);
     c.setBorder(Rectangle.NO_BORDER);
     tableVerif.addCell(c);
 }

 pair = false;
 for (int i = 0; i < stockIds.size(); i++) {
     Stock stock = stockRepository.findById(stockIds.get(i))
         .orElse(null);
     if (stock == null) continue;

     BaseColor bg = pair ? LIGHT_GREY : WHITE;
     ajouterLigneTableau(tableVerif,
         stock.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(tableVerif,
         quantites.get(i) + " "
         + (stock.getArticle().getUnite() != null
             ? stock.getArticle().getUnite() : ""),
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(tableVerif,
         "___________", bg, fontCell, Element.ALIGN_CENTER);

     PdfPCell confCell = new PdfPCell();
     confCell.setBackgroundColor(bg);
     confCell.setPadding(7);
     confCell.setBorder(Rectangle.NO_BORDER);
     confCell.addElement(new Paragraph("☐ OK  ☐ Écart", fontCell));
     tableVerif.addCell(confCell);
     pair = !pair;
 }
 doc.add(tableVerif);

 // Observations
 doc.add(Chunk.NEWLINE);
 PdfPTable zoneObs = new PdfPTable(1);
 zoneObs.setWidthPercentage(100);
 PdfPCell caseObs = new PdfPCell();
 caseObs.setBorder(Rectangle.BOX);
 caseObs.setBorderColor(new BaseColor(200, 200, 200));
 caseObs.setPadding(12);
 caseObs.addElement(new Paragraph(
     "Observations générales :\n\n\n\n", fontLabel));
 zoneObs.addCell(caseObs);
 doc.add(zoneObs);

 // Signatures
 doc.add(Chunk.NEWLINE);
 doc.add(Chunk.NEWLINE);
 PdfPTable sig = new PdfPTable(3);
 sig.setWidthPercentage(100);

 PdfPCell s1 = new PdfPCell();
 s1.setBorder(Rectangle.NO_BORDER);
 s1.addElement(new Paragraph("Signature du magasinier :", fontSig));
 s1.addElement(new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(s1);

 PdfPCell s2 = new PdfPCell();
 s2.setBorder(Rectangle.NO_BORDER);
 s2.setHorizontalAlignment(Element.ALIGN_CENTER);
 s2.addElement(new Paragraph("Cachet de l'entreprise :", fontSig));
 sig.addCell(s2);

 PdfPCell s3 = new PdfPCell();
 s3.setBorder(Rectangle.NO_BORDER);
 s3.setHorizontalAlignment(Element.ALIGN_RIGHT);
 s3.addElement(new Paragraph("Signature du responsable :", fontSig));
 s3.addElement(new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(s3);
 doc.add(sig);

 // Pied de page
 doc.add(Chunk.NEWLINE);
 Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
     Font.ITALIC, new BaseColor(150, 150, 150));
 Paragraph pied = new Paragraph(
     "Document généré le "
     + java.time.LocalDateTime.now().format(FMT)
     + " — Gestion Commerciale", fontPied);
 pied.setAlignment(Element.ALIGN_CENTER);
 doc.add(pied);

 doc.close();
 return out.toByteArray();
}

//=============================================
//BON DE LIVRAISON
//=============================================
public byte[] genererBonLivraison(Long bonId) throws Exception {

 BonLivraison bon = bonLivraisonRepository.findById(bonId)
     .orElseThrow(() -> new RuntimeException("Bon introuvable"));
 List<LigneVente> lignes =
     ligneVenteRepository.findByVenteId(bon.getVente().getId());

 Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
 ByteArrayOutputStream out = new ByteArrayOutputStream();
 PdfWriter.getInstance(doc, out);
 doc.open();

 Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   WHITE);
 Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, WHITE);
 Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
 Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
 Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);
 Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

 // Couleur statut
 BaseColor couleurStatut = bon.getStatut().equals("LIVRE")
     ? GREEN : new BaseColor(245, 124, 0);

 // En-tête
 PdfPTable header = new PdfPTable(2);
 header.setWidthPercentage(100);
 header.setWidths(new float[]{2f, 1f});

 PdfPCell cellG = new PdfPCell();
 cellG.setBackgroundColor(DARK_BLUE);
 cellG.setBorder(Rectangle.NO_BORDER);
 cellG.setPadding(15);
 Paragraph pG = new Paragraph("BON DE LIVRAISON\n", fontTitre);
 pG.add(new Phrase("Gestion Commerciale", fontSub));
 cellG.addElement(pG);
 header.addCell(cellG);

 PdfPCell cellD = new PdfPCell();
 cellD.setBackgroundColor(couleurStatut);
 cellD.setBorder(Rectangle.NO_BORDER);
 cellD.setPadding(15);
 Paragraph pD = new Paragraph(bon.getNumero() + "\n", fontTitre);
 pD.setAlignment(Element.ALIGN_RIGHT);
 Font fontStatut = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);
 pD.add(new Phrase(
     bon.getStatut().equals("LIVRE") ? "✓ LIVRÉ" : "⏳ EN ATTENTE",
     fontStatut));
 cellD.addElement(pD);
 header.addCell(cellD);
 doc.add(header);
 doc.add(Chunk.NEWLINE);

 // Infos
 PdfPTable infos = new PdfPTable(2);
 infos.setWidthPercentage(100);
 infos.setSpacingBefore(10);

 ajouterInfoLigne(infos, "N° Bon :",
     bon.getNumero(), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "N° Facture :",
     bon.getVente().getNumero(), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Date création :",
     bon.getDateCreation().format(FMT), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Client :",
     bon.getVente().getClient() != null
         ? bon.getVente().getClient().getNom()
         : "Client de passage",
     fontLabel, fontValeur);
 if (bon.getVente().getClient() != null
         && bon.getVente().getClient().getTelephone() != null) {
     ajouterInfoLigne(infos, "Téléphone :",
         bon.getVente().getClient().getTelephone(),
         fontLabel, fontValeur);
 }
 ajouterInfoLigne(infos, "Magasin de retrait :",
     bon.getMagasin().getNom()
     + " — " + bon.getMagasin().getAdresse(),
     fontLabel, fontValeur);
 if (bon.getDateLivraison() != null) {
     ajouterInfoLigne(infos, "Date livraison :",
         bon.getDateLivraison().format(FMT),
         fontLabel, fontValeur);
 }
 doc.add(infos);
 doc.add(Chunk.NEWLINE);

 // Tableau articles
 PdfPTable table = new PdfPTable(4);
 table.setWidthPercentage(100);
 table.setWidths(new float[]{3f, 1.2f, 1.5f, 1.8f});
 table.setSpacingBefore(10);

 for (String h : new String[]{
         "Désignation", "Quantité", "Prix U.", "Total"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(DARK_BLUE);
     c.setPadding(8);
     c.setBorder(Rectangle.NO_BORDER);
     table.addCell(c);
 }

 boolean pair = false;
 for (LigneVente l : lignes) {
     BaseColor bg = pair ? LIGHT_GREY : WHITE;
     ajouterLigneTableau(table,
         l.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         l.getQuantite() + " "
         + (l.getArticle().getUnite() != null
             ? l.getArticle().getUnite() : ""),
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         formatMontant(l.getPrixUnitaire()) + " FCFA",
         bg, fontCell, Element.ALIGN_RIGHT);
     ajouterLigneTableau(table,
         formatMontant(l.getMontantLigne()) + " FCFA",
         bg, fontCell, Element.ALIGN_RIGHT);
     pair = !pair;
 }
 doc.add(table);

 // Total
 PdfPTable totalTable = new PdfPTable(2);
 totalTable.setWidthPercentage(40);
 totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
 totalTable.setSpacingBefore(10);

 PdfPCell lTot = new PdfPCell(new Phrase("TOTAL PAYÉ", fontTotal));
 lTot.setBackgroundColor(DARK_BLUE);
 lTot.setPadding(10);
 lTot.setBorder(Rectangle.NO_BORDER);
 totalTable.addCell(lTot);

 PdfPCell vTot = new PdfPCell(new Phrase(
     formatMontant(bon.getVente().getMontantTotal()) + " FCFA",
     fontTotal));
 vTot.setBackgroundColor(GREEN);
 vTot.setPadding(10);
 vTot.setBorder(Rectangle.NO_BORDER);
 vTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
 totalTable.addCell(vTot);
 doc.add(totalTable);

 // Mention retrait
 doc.add(Chunk.NEWLINE);
 PdfPTable mention = new PdfPTable(1);
 mention.setWidthPercentage(100);
 PdfPCell mentionCell = new PdfPCell();
 mentionCell.setBackgroundColor(new BaseColor(255, 243, 205));
 mentionCell.setBorder(Rectangle.BOX);
 mentionCell.setBorderColor(new BaseColor(245, 124, 0));
 mentionCell.setPadding(12);
 Font fontMention = new Font(Font.FontFamily.HELVETICA, 10,
     Font.BOLD, new BaseColor(120, 60, 0));
 Paragraph pMention = new Paragraph(
     "📦 Ce bon de livraison doit être présenté au magasin :\n"
     + bon.getMagasin().getNom()
     + " — " + bon.getMagasin().getAdresse(), fontMention);
 pMention.setAlignment(Element.ALIGN_CENTER);
 mentionCell.addElement(pMention);
 mention.addCell(mentionCell);
 doc.add(mention);

 // Signatures
 doc.add(Chunk.NEWLINE);
 doc.add(Chunk.NEWLINE);
 PdfPTable sig = new PdfPTable(2);
 sig.setWidthPercentage(100);

 PdfPCell sigClient = new PdfPCell();
 sigClient.setBorder(Rectangle.NO_BORDER);
 sigClient.addElement(new Paragraph("Signature du client :", fontSig));
 sigClient.addElement(
     new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(sigClient);

 PdfPCell sigMag = new PdfPCell();
 sigMag.setBorder(Rectangle.NO_BORDER);
 sigMag.setHorizontalAlignment(Element.ALIGN_RIGHT);
 sigMag.addElement(new Paragraph("Signature du magasinier :", fontSig));
 sigMag.addElement(
     new Paragraph("\n\n_______________________", fontSig));
 sig.addCell(sigMag);
 doc.add(sig);

 // Pied de page
 doc.add(Chunk.NEWLINE);
 Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
     Font.ITALIC, new BaseColor(150, 150, 150));
 Paragraph pied = new Paragraph(
     "Document généré le "
     + java.time.LocalDateTime.now().format(FMT)
     + " — Gestion Commerciale", fontPied);
 pied.setAlignment(Element.ALIGN_CENTER);
 doc.add(pied);

 doc.close();
 return out.toByteArray();
}

//=============================================
//RAPPORT INVENTAIRE PDF
//=============================================
public byte[] genererRapportInventaire(Long inventaireId)
     throws Exception {

 Inventaire inventaire = inventaireRepository.findById(inventaireId)
     .orElseThrow(() -> new RuntimeException("Inventaire introuvable"));

 Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
 ByteArrayOutputStream out = new ByteArrayOutputStream();
 PdfWriter.getInstance(doc, out);
 doc.open();

 Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE);
 Font fontSub    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, WHITE);
 Font fontLabel  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   DARK_BLUE);
 Font fontValeur = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   WHITE);
 Font fontCell   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);
 Font fontTotal  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   WHITE);
 Font fontSig    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, DARK_BLUE);

 // En-tête
 PdfPTable header = new PdfPTable(2);
 header.setWidthPercentage(100);
 header.setWidths(new float[]{2f, 1f});

 PdfPCell cellG = new PdfPCell();
 cellG.setBackgroundColor(DARK_BLUE);
 cellG.setBorder(Rectangle.NO_BORDER);
 cellG.setPadding(15);
 Paragraph pG = new Paragraph("RAPPORT D'INVENTAIRE\n", fontTitre);
 pG.add(new Phrase(
     "Gestion Commerciale — " + inventaire.getLocal().getNom(),
     fontSub));
 cellG.addElement(pG);
 header.addCell(cellG);

 PdfPCell cellD = new PdfPCell();
 cellD.setBackgroundColor(GREEN);
 cellD.setBorder(Rectangle.NO_BORDER);
 cellD.setPadding(15);
 Paragraph pD = new Paragraph(inventaire.getType(), fontTitre);
 pD.setAlignment(Element.ALIGN_RIGHT);
 cellD.addElement(pD);
 header.addCell(cellD);
 doc.add(header);
 doc.add(Chunk.NEWLINE);

 // Infos
 PdfPTable infos = new PdfPTable(2);
 infos.setWidthPercentage(100);
 infos.setSpacingBefore(10);
 ajouterInfoLigne(infos, "Local :",
     inventaire.getLocal().getNom(), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Type :",
     inventaire.getType(), fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Date inventaire :",
     inventaire.getDateInventaire().format(FMT),
     fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Date validation :",
     inventaire.getDateValidation() != null
         ? inventaire.getDateValidation().format(FMT) : "-",
     fontLabel, fontValeur);
 ajouterInfoLigne(infos, "Effectué par :",
     inventaire.getUtilisateur().getPrenom()
     + " " + inventaire.getUtilisateur().getNom(),
     fontLabel, fontValeur);
 doc.add(infos);
 doc.add(Chunk.NEWLINE);

 // Tableau
 PdfPTable table = new PdfPTable(6);
 table.setWidthPercentage(100);
 table.setWidths(new float[]{1.2f, 3f, 1f, 1.2f, 1.2f, 1.5f});
 table.setSpacingBefore(10);

 for (String h : new String[]{
         "Référence", "Désignation", "Unité",
         "Qté théorique", "Qté réelle", "Écart"}) {
     PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
     c.setBackgroundColor(DARK_BLUE);
     c.setPadding(8); c.setBorder(Rectangle.NO_BORDER);
     table.addCell(c);
 }

 java.math.BigDecimal totalEcartNegatif = java.math.BigDecimal.ZERO;
 boolean pair = false;
 for (InventaireLigne ligne : inventaire.getLignes()) {
     BaseColor bg = pair ? LIGHT_GREY : WHITE;

     // Colorer les lignes avec écart
     if (ligne.getEcart() != null && ligne.getEcart() < 0) {
         bg = new BaseColor(255, 235, 235);
     } else if (ligne.getEcart() != null
             && ligne.getEcart() > 0) {
         bg = new BaseColor(235, 255, 240);
     }

     ajouterLigneTableau(table,
         ligne.getArticle().getReference(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         ligne.getArticle().getDesignation(),
         bg, fontCell, Element.ALIGN_LEFT);
     ajouterLigneTableau(table,
         ligne.getArticle().getUnite() != null
             ? ligne.getArticle().getUnite() : "-",
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         String.valueOf(ligne.getQteTheorique()),
         bg, fontCell, Element.ALIGN_CENTER);
     ajouterLigneTableau(table,
         ligne.getQteReelle() != null
             ? String.valueOf(ligne.getQteReelle()) : "-",
         bg, fontCell, Element.ALIGN_CENTER);

     // Écart coloré
     PdfPCell ecartCell = new PdfPCell();
     ecartCell.setBackgroundColor(bg);
     ecartCell.setPadding(7);
     ecartCell.setBorder(Rectangle.NO_BORDER);
     ecartCell.setHorizontalAlignment(Element.ALIGN_CENTER);
     if (ligne.getEcart() != null) {
         BaseColor couleurEcart = ligne.getEcart() < 0
             ? new BaseColor(192, 57, 43)
             : (ligne.getEcart() > 0
                 ? new BaseColor(39, 174, 96)
                 : new BaseColor(150, 150, 150));
         Font fontEcart = new Font(Font.FontFamily.HELVETICA,
             9, Font.BOLD, couleurEcart);
         String texteEcart = ligne.getEcart() > 0
             ? "+" + ligne.getEcart() : String.valueOf(ligne.getEcart());
         ecartCell.addElement(new Paragraph(texteEcart, fontEcart));

         if (ligne.getEcart() < 0 && ligne.getValeurEcart() != null) {
             totalEcartNegatif = totalEcartNegatif
                 .add(ligne.getValeurEcart());
         }
     } else {
         ecartCell.addElement(new Paragraph("-", fontCell));
     }
     table.addCell(ecartCell);
     pair = !pair;
 }
 doc.add(table);

 // Résumé écarts
 doc.add(Chunk.NEWLINE);
 PdfPTable resume = new PdfPTable(2);
 resume.setWidthPercentage(50);
 resume.setHorizontalAlignment(Element.ALIGN_RIGHT);

 long nbManques = inventaire.getLignes().stream()
     .filter(l -> l.getEcart() != null && l.getEcart() < 0)
     .count();
 long nbSurplus = inventaire.getLignes().stream()
     .filter(l -> l.getEcart() != null && l.getEcart() > 0)
     .count();
 long nbConformes = inventaire.getLignes().stream()
     .filter(l -> l.getEcart() != null && l.getEcart() == 0)
     .count();

 ajouterRecapLigne(resume,
     "Articles conformes", nbConformes + " réf.",
     fontLabel, fontValeur);
 ajouterRecapLigne(resume,
     "Articles en surplus", nbSurplus + " réf.",
     fontLabel, fontValeur);
 ajouterRecapLigne(resume,
     "Articles en manque", nbManques + " réf.",
     fontLabel, fontValeur);

 PdfPCell lTot = new PdfPCell(
     new Phrase("VALEUR ÉCARTS NÉGATIFS", fontTotal));
 lTot.setBackgroundColor(new BaseColor(192, 57, 43));
 lTot.setPadding(10); lTot.setBorder(Rectangle.NO_BORDER);
 resume.addCell(lTot);

 PdfPCell vTot = new PdfPCell(
     new Phrase(formatMontant(totalEcartNegatif) + " FCFA",
         fontTotal));
 vTot.setBackgroundColor(DARK_BLUE);
 vTot.setPadding(10); vTot.setBorder(Rectangle.NO_BORDER);
 vTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
 resume.addCell(vTot);

 doc.add(resume);

 // Observations
 if (inventaire.getObservations() != null
         && !inventaire.getObservations().isEmpty()) {
     doc.add(Chunk.NEWLINE);
     PdfPTable obs = new PdfPTable(1);
     obs.setWidthPercentage(100);
     PdfPCell obsCell = new PdfPCell();
     obsCell.setBorder(Rectangle.BOX);
     obsCell.setBorderColor(new BaseColor(200, 200, 200));
     obsCell.setPadding(10);
     obsCell.addElement(new Paragraph(
         "Observations : " + inventaire.getObservations(),
         fontValeur));
     obs.addCell(obsCell);
     doc.add(obs);
 }

 // Signatures
 doc.add(Chunk.NEWLINE);
 doc.add(Chunk.NEWLINE);
 PdfPTable sig = new PdfPTable(2);
 sig.setWidthPercentage(100);

 PdfPCell s1 = new PdfPCell();
 s1.setBorder(Rectangle.NO_BORDER);
 s1.addElement(new Paragraph(
     "Signature du responsable :", fontSig));
 s1.addElement(new Paragraph(
     inventaire.getUtilisateur().getPrenom()
     + " " + inventaire.getUtilisateur().getNom(),
     fontSig));
 s1.addElement(new Paragraph(
     "\n\n_______________________", fontSig));
 sig.addCell(s1);

 PdfPCell s2 = new PdfPCell();
 s2.setBorder(Rectangle.NO_BORDER);
 s2.setHorizontalAlignment(Element.ALIGN_RIGHT);
 s2.addElement(new Paragraph("Cachet de l'entreprise :", fontSig));
 s2.addElement(new Paragraph("\n\n", fontSig));
 sig.addCell(s2);

 doc.add(sig);

 // Pied de page
 doc.add(Chunk.NEWLINE);
 Font fontPied = new Font(Font.FontFamily.HELVETICA, 8,
     Font.ITALIC, new BaseColor(150, 150, 150));
 doc.add(new Paragraph(
     "Document généré le "
     + java.time.LocalDateTime.now().format(FMT)
     + " — Gestion Commerciale", fontPied));

 doc.close();
 return out.toByteArray();
}


}