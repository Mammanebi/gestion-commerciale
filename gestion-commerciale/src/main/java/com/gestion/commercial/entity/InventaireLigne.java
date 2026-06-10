package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "inventaire_ligne")
public class InventaireLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventaire_id")
    private Inventaire inventaire;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(nullable = false)
    private Integer qteTheorique;

    // Saisie par le responsable/admin
    private Integer qteReelle;

    // Calculé automatiquement : qteReelle - qteTheorique
    private Integer ecart;

    // Valeur de l'écart en FCFA
    @Column(precision = 10, scale = 2)
    private BigDecimal valeurEcart;
}