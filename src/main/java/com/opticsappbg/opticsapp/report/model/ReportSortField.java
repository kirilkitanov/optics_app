package com.opticsappbg.opticsapp.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportSortField {

    DATE("Дата"),
    DOCUMENT("Документ"),
    PRODUCT("Продукт"),
    CATEGORY("Категория"),
    CUSTOMER("Клиент"),
    SUPPLIER("Доставчик"),
    USER("Потребител");

    private final String displayName;
}