package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mouvement_stock")
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // ENTREE, SORTIE, TRANSFERT, LIVRAISON_CLIENT
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer quantite;

    // EN_ATTENTE, APPROUVE, REJETE
    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    private String motif;

    // ✅ NOUVEAU — Local de destination (boutique ou null si livraison client)
    @ManyToOne
    @JoinColumn(name = "local_destination_id")
    private Local localDestination;

    // ✅ NOUVEAU — Type de mouvement métier
    // APPROVISIONNEMENT_BOUTIQUE : magasin → boutique
    // LIVRAISON_CLIENT : magasin → client (via bon de sortie)
    // VENTE_BOUTIQUE : boutique → client (vente normale)
    @Column(name = "type_mouvement")
    private String typeMouvement;

    @Column(nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();
}