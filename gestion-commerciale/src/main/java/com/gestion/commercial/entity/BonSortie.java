package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bon_sortie")
public class BonSortie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "mouvement_id")
    private MouvementStock mouvement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;

    @Column(nullable = false, unique = true)
    private String numero;

    private LocalDateTime dateApprobation;

    private String observation;
}