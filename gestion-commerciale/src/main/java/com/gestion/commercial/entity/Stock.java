package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_id")
    private Local local;

    @Column(nullable = false)
    private Integer quantite = 0;

    @Column(name = "quantite_reservee", nullable = false)
    private Integer quantiteReservee = 0;

    @Column(name = "quantite_min")
    private Integer quantiteMin = 0;

    // Quantité réellement disponible
    public Integer getQuantiteDisponible() {
        return quantite - (quantiteReservee != null ? quantiteReservee : 0);
    }
}