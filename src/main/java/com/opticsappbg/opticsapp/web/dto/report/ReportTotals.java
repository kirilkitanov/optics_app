package com.opticsappbg.opticsapp.web.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReportTotals {

    private Long quantity;
    private Long purchaseCount;

    private BigDecimal salesAmount;
    private BigDecimal deliveryAmount;
    private BigDecimal purchaseValue;
    private BigDecimal profit;

    private BigDecimal receivedAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private BigDecimal netAmount;

    private Long increaseQuantity;
    private Long decreaseQuantity;
    private Long netDifference;

    private BigDecimal differencePurchaseValue;
    private BigDecimal differenceSellingValue;
}
