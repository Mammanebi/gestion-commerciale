package com.gestion.commercial.entity;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "local")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    // MAGASIN ou BOUTIQUE
    @Column(nullable = false)
    private String type;

    private String adresse;

    @OneToMany(mappedBy = "local", fetch = FetchType.EAGER)
    private List<Stock> stocks = new ArrayList<>();
}