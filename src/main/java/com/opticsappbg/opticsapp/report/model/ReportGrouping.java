package com.opticsappbg.opticsapp.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportGrouping {

    NONE("Без групиране"),
    DOCUMENT("Документ"),
    DAY("Ден"),
    WEEK("Седмица"),
    MONTH("Месец"),
    YEAR("Година"),
    PRODUCT("Продукт"),
    CATEGORY("Категория"),
    CUSTOMER("Клиент"),
    SUPPLIER("Доставчик"),
    USER("Потребител");

    private final String displayName;
}
