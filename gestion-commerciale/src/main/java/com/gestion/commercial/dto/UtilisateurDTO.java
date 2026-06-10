package com.gestion.commercial.dto;

import com.gestion.commercial.entity.Utilisateur;
import lombok.Data;

@Data
public class UtilisateurDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String username;
    private String role;
    private Boolean actif;

    // ✅ NOUVEAU
    private String localAffecteNom;
    private String localAffecteType;
    private Long localAffecteId;

    public UtilisateurDTO(Utilisateur u) {
        this.id       = u.getId();
        this.nom      = u.getNom();
        this.prenom   = u.getPrenom();
        this.username = u.getUsername();
        this.role     = u.getRole();
        this.actif    = u.getActif();
        if (u.getLocalAffecte() != null) {
            this.localAffecteNom  = u.getLocalAffecte().getNom();
            this.localAffecteType = u.getLocalAffecte().getType();
            this.localAffecteId   = u.getLocalAffecte().getId();
        }
    }
}