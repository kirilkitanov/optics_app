package com.opticsappbg.opticsapp.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportMetric {

    QUANTITY("Бройки"),
    PURCHASE_COUNT("Брой покупки"),
    SALES_AMOUNT("Продажна стойност"),
    DELIVERY_AMOUNT("Доставна стойност"),
    PURCHASE_VALUE("Себестойност"),
    PROFIT("Печалба"),
    RECEIVED_AMOUNT("Получени суми"),
    PAID_AMOUNT("Извършени суми"),
    REFUNDED_AMOUNT("Върнати суми"),
    NET_AMOUNT("Нетна сума"),
    INCREASE_QUANTITY("Увеличение"),
    DECREASE_QUANTITY("Намаление"),
    NET_DIFFERENCE("Нетна разлика"),
    DIFFERENCE_PURCHASE_VALUE("Доставна стойност на разликата"),
    DIFFERENCE_SELLING_VALUE("Продажна стойност на разликата");

    private final String displayName;
}
