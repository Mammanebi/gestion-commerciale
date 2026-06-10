package com.gestion.commercial.service;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionJourneeService {

    private final SessionJourneeRepository sessionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final LocalRepository localRepository;
    private final VenteRepository venteRepository;
    
    
    

    // Ouvrir la journée
    @Transactional
    public SessionJournee ouvrirSession(Long utilisateurId,
                                        Long localId,
                                        BigDecimal montantOuverture) {

        // ✅ Vérifier uniquement pour CE local spécifique
        sessionRepository.findByLocalIdAndStatut(localId, "OUVERTE")
            .ifPresent(s -> {
                throw new RuntimeException(
                    "Une session est déjà ouverte pour ce local : "
                    + s.getLocal().getNom());
            });

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Local local = localRepository.findById(localId)
            .orElseThrow(() -> new RuntimeException("Local introuvable"));

        SessionJournee session = new SessionJournee();
        session.setUtilisateur(utilisateur);
        session.setLocal(local);
        session.setMontantOuverture(montantOuverture);
        session.setDateOuverture(LocalDateTime.now());
        session.setStatut("OUVERTE");

        return sessionRepository.save(session);
    }

    // Clôturer la journée
    @Transactional
    public SessionJournee cloturerSession(Long sessionId) {
        SessionJournee session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session introuvable"));

        if (!session.getStatut().equals("OUVERTE")) {
            throw new RuntimeException("Cette session est déjà clôturée");
        }

        // Calculer le total encaissé dans la journée
        List<Vente> ventes = venteRepository.findBySessionId(sessionId);
        BigDecimal totalVentes = ventes.stream()
            .filter(v -> v.getStatut().equals("VALIDEE"))
            .map(Vente::getMontantTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setMontantCloture(
            session.getMontantOuverture().add(totalVentes));
        session.setDateCloture(LocalDateTime.now());
        session.setStatut("CLOTUREE");

        return sessionRepository.save(session);
    }

    // Récupérer la session ouverte d'un local
    public SessionJournee getSessionOuverte(Long localId) {
        return sessionRepository.findByLocalIdAndStatut(localId, "OUVERTE")
            .orElseThrow(() -> new RuntimeException(
                "Aucune session ouverte pour ce local"));
    }

    // Récupérer toutes les sessions
    public List<SessionJournee> getToutesLesSessions() {
        return sessionRepository.findAll();
    }
}