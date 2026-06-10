package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "reception")
public class Reception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_id")
    private Local local;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // EN_COURS, VALIDEE
    @Column(nullable = false)
    private String statut = "EN_COURS";

    @Column(nullable = false)
    private LocalDateTime dateReception = LocalDateTime.now();

    private String reference; // numéro bon commande fournisseur (optionnel)
    private String fournisseur;

    @OneToMany(mappedBy = "reception",
               cascade = CascadeType.ALL,
               fetch = FetchType.EAGER)
    private List<ReceptionLigne> lignes = new ArrayList<>();
}