package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vente")
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private SessionJournee session;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime dateVente = LocalDateTime.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    // DETAIL, GROS
    @Column(name = "type_vente")
    private String typeVente = "DETAIL";

    // ✅ NOUVEAU — magasin source si stock boutique insuffisant
    @ManyToOne
    @JoinColumn(name = "source_logistique_id")
    private Local sourceLogistique;

    // EN_COURS, VALIDEE, EN_ATTENTE_RETRAIT, LIVREE, ANNULEE
    @Column(nullable = false)
    private String statut = "EN_COURS";

    // ✅ NOUVEAU — message affiché sur la facture
    @Column(name = "mention_livraison")
    private String mentionLivraison;
}