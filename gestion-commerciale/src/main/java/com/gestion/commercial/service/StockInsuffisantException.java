package com.gestion.commercial.service;

import lombok.Getter;

@Getter
public class StockInsuffisantException extends RuntimeException {

    private final Long articleId;
    private final Long localId;
    private final Integer quantiteDemandee;
    private final Integer quantiteDisponible;

    public StockInsuffisantException(Long articleId, Long localId,
                                      Integer quantiteDemandee,
                                      Integer quantiteDisponible) {
        super("Stock boutique insuffisant");
        this.articleId = articleId;
        this.localId = localId;
        this.quantiteDemandee = quantiteDemandee;
        this.quantiteDisponible = quantiteDisponible;
    }
}