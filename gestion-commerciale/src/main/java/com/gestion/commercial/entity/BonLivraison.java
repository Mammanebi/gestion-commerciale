package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bon_livraison")
public class BonLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "magasin_id")
    private Local magasin;

    // EN_ATTENTE, EN_PREPARATION, LIVRE
    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateLivraison;

    @ManyToOne
    @JoinColumn(name = "magasinier_id")
    private Utilisateur magasinier;

    private String observations;
}