package com.gestion.commercial.dto;

import com.gestion.commercial.entity.Vente;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VenteDTO {

    private Long id;
    private String numero;
    private String clientNom;
    private String boutiqueNom;
    private BigDecimal montantTotal;
    private String statut;
    private LocalDateTime dateVente;

    public VenteDTO(Vente v) {
        this.id          = v.getId();
        this.numero      = v.getNumero();
        this.clientNom   = v.getClient() != null
            ? v.getClient().getNom() : "Client de passage";
        this.boutiqueNom = v.getSession().getLocal().getNom();
        this.montantTotal = v.getMontantTotal();
        this.statut      = v.getStatut();
        this.dateVente   = v.getDateVente();
    }
}