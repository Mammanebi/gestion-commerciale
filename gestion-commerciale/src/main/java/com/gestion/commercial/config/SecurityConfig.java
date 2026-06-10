package com.gestion.commercial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http
        .authorizeHttpRequests(auth -> auth
        	    .requestMatchers("/css/**", "/js/**", "/images/**")
        	        .permitAll()  

        	    .requestMatchers("/admin/**")
        	        .hasRole("ADMIN")
        	        
        	    .requestMatchers("/inventaires/**")
        	        .hasAnyRole("RESPONSABLE", "ADMIN")

        	    .requestMatchers("/ventes/**")
        	        .hasAnyRole("CAISSIER", "ADMIN")

        	    .requestMatchers("/livraisons/confirmer/**")
        	        .hasAnyRole("MAGASINIER", "RESPONSABLE", "ADMIN")
        	    .requestMatchers("/livraisons/**")
        	        .hasAnyRole("CAISSIER", "MAGASINIER", "RESPONSABLE", "ADMIN")

        	    .requestMatchers("/mouvements/approuver/**", "/mouvements/rejeter/**")
        	        .hasAnyRole("RESPONSABLE", "ADMIN", "MAGASINIER")
        	    .requestMatchers("/mouvements/**")
        	        .hasAnyRole("CAISSIER", "MAGASINIER", "RESPONSABLE", "ADMIN")

        	    .requestMatchers("/pdf/rapport-mensuel/**")
        	        .hasAnyRole("RESPONSABLE", "ADMIN")

        	    .requestMatchers("/articles/**")
        	        .hasAnyRole("CAISSIER", "MAGASINIER", "RESPONSABLE", "ADMIN")

        	    .requestMatchers("/bons-sortie/**", "/receptions/**")
        	        .hasAnyRole("CAISSIER", "MAGASINIER", "RESPONSABLE", "ADMIN")

        	    .requestMatchers("/pdf/**")  // ✅ Une seule fois ici
        	        .hasAnyRole("CAISSIER", "MAGASINIER", "RESPONSABLE", "ADMIN")

        	    .anyRequest().authenticated()
        	)
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/logout")
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}