package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String motDePasse;

    // ADMIN, RESPONSABLE, MAGASINIER, CAISSIER
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Boolean actif = true;

    // ✅ NOUVEAU — Local affecté (boutique ou magasin)
    // Obligatoire pour CAISSIER et MAGASINIER
    // Null pour ADMIN et RESPONSABLE
    @ManyToOne
    @JoinColumn(name = "local_affecte_id")
    private Local localAffecte;
}