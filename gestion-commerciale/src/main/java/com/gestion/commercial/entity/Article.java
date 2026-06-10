package com.gestion.commercial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String designation;

    private String unite;

    @Column(precision = 10, scale = 2)
    private BigDecimal prixAchat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixVente;
}