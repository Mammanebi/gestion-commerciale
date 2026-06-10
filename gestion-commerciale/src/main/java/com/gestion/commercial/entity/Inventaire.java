package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inventaire")
public class Inventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_id")
    private Local local;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // MENSUEL, TRIMESTRIEL, ANNUEL
    @Column(nullable = false)
    private String type;

    // EN_COURS, VALIDE
    @Column(nullable = false)
    private String statut = "EN_COURS";

    @Column(nullable = false)
    private LocalDateTime dateInventaire = LocalDateTime.now();

    private LocalDateTime dateValidation;

    private String observations;

    @OneToMany(mappedBy = "inventaire",
               cascade = CascadeType.ALL,
               fetch = FetchType.EAGER)
    private List<InventaireLigne> lignes = new ArrayList<>();
}