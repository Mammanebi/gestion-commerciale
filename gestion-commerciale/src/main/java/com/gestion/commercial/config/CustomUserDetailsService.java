package com.gestion.commercial.config;

import com.gestion.commercial.entity.Utilisateur;
import com.gestion.commercial.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Utilisateur introuvable : " + username));

        return new org.springframework.security.core.userdetails.User(
            utilisateur.getUsername(),
            utilisateur.getMotDePasse(),
            utilisateur.getActif(),
            true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole()))
        );
    }
}