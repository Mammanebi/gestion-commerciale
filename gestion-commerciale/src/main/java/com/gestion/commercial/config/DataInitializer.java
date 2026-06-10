package com.gestion.commercial.config;

import com.gestion.commercial.entity.*;
import com.gestion.commercial.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final LocalRepository localRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Créer l'admin si inexistant
        if (!utilisateurRepository.existsByUsername("admin")) {
            Utilisateur admin = new Utilisateur();
            admin.setNom("Admin");
            admin.setPrenom("Système");
            admin.setUsername("admin");
            admin.setMotDePasse(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setActif(true);
            utilisateurRepository.save(admin);
            System.out.println("✅ Utilisateur admin créé : admin / admin123");
        }

        // Créer les locaux si inexistants
        if (localRepository.count() == 0) {
            Local mag1 = new Local();
            mag1.setNom("Magasin Principal");
            mag1.setType("MAGASIN");
            mag1.setAdresse("Douala - Zone Industrielle");
            localRepository.save(mag1);

            Local mag2 = new Local();
            mag2.setNom("Magasin Secondaire");
            mag2.setType("MAGASIN");
            mag2.setAdresse("Douala - Bonabéri");
            localRepository.save(mag2);

            Local bout1 = new Local();
            bout1.setNom("Boutique Akwa");
            bout1.setType("BOUTIQUE");
            bout1.setAdresse("Douala - Akwa");
            localRepository.save(bout1);

            Local bout2 = new Local();
            bout2.setNom("Boutique Deido");
            bout2.setType("BOUTIQUE");
            bout2.setAdresse("Douala - Deido");
            localRepository.save(bout2);

            System.out.println("✅ Locaux créés : 2 magasins, 2 boutiques");
        }
    }
}