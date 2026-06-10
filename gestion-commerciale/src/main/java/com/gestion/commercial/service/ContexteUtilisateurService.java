package com.gestion.commercial.service;

import com.gestion.commercial.entity.Utilisateur;
import com.gestion.commercial.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContexteUtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur getUtilisateurConnecte(Authentication auth) {
        return utilisateurRepository
            .findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException(
                "Utilisateur introuvable"));
    }

    public boolean estCaissier(Authentication auth) {
        return "CAISSIER".equals(
            getUtilisateurConnecte(auth).getRole());
    }

    public boolean estMagasinier(Authentication auth) {
        return "MAGASINIER".equals(
            getUtilisateurConnecte(auth).getRole());
    }

    public boolean estResponsable(Authentication auth) {
        return "RESPONSABLE".equals(
            getUtilisateurConnecte(auth).getRole());
    }

    public boolean estAdmin(Authentication auth) {
        return "ADMIN".equals(
            getUtilisateurConnecte(auth).getRole());
    }

    // Local affecté à l'utilisateur connecté
    public Long getLocalAffecteId(Authentication auth) {
        Utilisateur u = getUtilisateurConnecte(auth);
        return u.getLocalAffecte() != null
            ? u.getLocalAffecte().getId() : null;
    }
}