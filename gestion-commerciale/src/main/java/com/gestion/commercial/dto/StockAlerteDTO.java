package com.gestion.commercial.dto;

import com.gestion.commercial.entity.Stock;
import lombok.Data;

@Data
public class StockAlerteDTO {

    private String articleDesignation;
    private String articleReference;
    private String localNom;
    private String localType;
    private Integer quantite;
    private String unite;

    public StockAlerteDTO(Stock s) {
        this.articleDesignation = s.getArticle().getDesignation();
        this.articleReference   = s.getArticle().getReference();
        this.localNom           = s.getLocal().getNom();
        this.localType          = s.getLocal().getType();
        this.quantite           = s.getQuantite();
        this.unite              = s.getArticle().getUnite();
    }
}