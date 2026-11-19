package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.report.model.ReportMetric;
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
    private BigDecimal documentAmount;
    private BigDecimal outstandingAmount;
    private String paymentStatus;

    private Long increaseQuantity;
    private Long decreaseQuantity;
    private Long netDifference;

    private BigDecimal differencePurchaseValue;
    private BigDecimal differenceSellingValue;

    public Object getMetricValue(ReportMetric metric) {
        if (metric == null) {
            return null;
        }

        return switch (metric) {
            case QUANTITY -> quantity;
            case PURCHASE_COUNT -> purchaseCount;
            case SALES_AMOUNT -> salesAmount;
            case DELIVERY_AMOUNT -> deliveryAmount;
            case PURCHASE_VALUE -> purchaseValue;
            case PROFIT -> profit;
            case RECEIVED_AMOUNT -> receivedAmount;
            case PAID_AMOUNT -> paidAmount;
            case REFUNDED_AMOUNT -> refundedAmount;
            case NET_AMOUNT -> netAmount;
            case INCREASE_QUANTITY -> increaseQuantity;
            case DECREASE_QUANTITY -> decreaseQuantity;
            case NET_DIFFERENCE -> netDifference;
            case DIFFERENCE_PURCHASE_VALUE -> differencePurchaseValue;
            case DIFFERENCE_SELLING_VALUE -> differenceSellingValue;
        };
    }
}
