package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session_journee")
public class SessionJournee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_id")
    private Local local;

    @Column(nullable = false)
    private LocalDateTime dateOuverture = LocalDateTime.now();

    private LocalDateTime dateCloture;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantOuverture = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantCloture;

    // OUVERTE, CLOTUREE
    @Column(nullable = false)
    private String statut = "OUVERTE";
}