package com.gestion.commercial.dto;

import lombok.Data;

@Data
public class NouvelUtilisateurDTO {

    private String nom;
    private String prenom;
    private String username;
    private String motDePasse;
    private String confirmerMotDePasse;
    private String role;

    // ✅ NOUVEAU
    private Long localAffecteId;

    public boolean motDePasseValide() {
        return motDePasse != null
            && motDePasse.equals(confirmerMotDePasse)
            && motDePasse.length() >= 6;
    }

    // ✅ Local obligatoire pour CAISSIER et MAGASINIER
    public boolean localValide() {
        if ("CAISSIER".equals(role) || "MAGASINIER".equals(role)) {
            return localAffecteId != null;
        }
        return true;
    }
}