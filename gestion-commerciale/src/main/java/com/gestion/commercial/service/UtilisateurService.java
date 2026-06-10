package com.gestion.commercial.service;

import com.gestion.commercial.dto.NouvelUtilisateurDTO;
import com.gestion.commercial.dto.UtilisateurDTO;
import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final LocalRepository localRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Utilisateur> getTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public Utilisateur creerUtilisateur(NouvelUtilisateurDTO dto) {
        if (utilisateurRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException(
                "Ce nom d'utilisateur est déjà pris : " + dto.getUsername());
        }
        if (!dto.motDePasseValide()) {
            throw new RuntimeException(
                "Les mots de passe ne correspondent pas "
                + "ou sont trop courts (min. 6 caractères)");
        }
        if (!dto.localValide()) {
            throw new RuntimeException(
                "Un local doit être affecté pour le rôle "
                + dto.getRole());
        }

        Utilisateur u = new Utilisateur();
        u.setNom(dto.getNom());
        u.setPrenom(dto.getPrenom());
        u.setUsername(dto.getUsername());
        u.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        u.setRole(dto.getRole());
        u.setActif(true);

        // ✅ Affecter le local si fourni
        if (dto.getLocalAffecteId() != null) {
            Local local = localRepository.findById(dto.getLocalAffecteId())
                .orElseThrow(() -> new RuntimeException("Local introuvable"));

            // Vérifier cohérence rôle / type de local
            if ("CAISSIER".equals(dto.getRole())
                    && !"BOUTIQUE".equals(local.getType())) {
                throw new RuntimeException(
                    "Un caissier doit être affecté à une BOUTIQUE");
            }
            if ("MAGASINIER".equals(dto.getRole())
                    && !"MAGASIN".equals(local.getType())) {
                throw new RuntimeException(
                    "Un magasinier doit être affecté à un MAGASIN");
            }
            u.setLocalAffecte(local);
        }

        return utilisateurRepository.save(u);
    }

    @org.springframework.transaction.annotation.Transactional
    public Utilisateur toggleActif(Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        u.setActif(!u.getActif());
        return utilisateurRepository.save(u);
    }

    @org.springframework.transaction.annotation.Transactional
    public Utilisateur reinitialiserMotDePasse(Long id,
                                                String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 6) {
            throw new RuntimeException(
                "Le mot de passe doit faire au moins 6 caractères");
        }
        Utilisateur u = utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        u.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        return utilisateurRepository.save(u);
    }

    public void supprimer(Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (u.getRole().equals("ADMIN")) {
            throw new RuntimeException(
                "Impossible de supprimer un administrateur");
        }
        utilisateurRepository.deleteById(id);
    }

    // ✅ NOUVEAU — Récupérer utilisateur depuis username
    public Utilisateur getParUsername(String username) {
        return utilisateurRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}