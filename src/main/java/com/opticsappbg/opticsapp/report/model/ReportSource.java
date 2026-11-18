package com.opticsappbg.opticsapp.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportSource {

    SALES("Продажби"),
    DELIVERIES("Доставки"),
    CUSTOMER_PAYMENTS("Клиентски плащания"),
    SUPPLIER_PAYMENTS("Плащания към доставчици"),
    STOCKTAKES("Ревизии"),
    STOCK_ADJUSTMENTS("Складови корекции");

    private final String displayName;
}
