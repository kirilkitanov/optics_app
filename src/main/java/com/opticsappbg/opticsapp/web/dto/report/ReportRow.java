package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import com.opticsappbg.opticsapp.report.model.ReportMetric;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReportRow {

    private LocalDateTime date;

    private Long documentId;
    private String documentNumber;
    private String documentType;

    private Long productId;
    private String productName;
    private String categoryName;

    private Long customerId;
    private String customerName;
    private String supplierName;
    private String username;
    private String reason;
    private String groupLabel;

    private PaymentMethod paymentMethod;
    private String paymentsSummary;
    private String paymentMethodsSummary;

    private Long quantity;
    private Long purchaseCount;

    private BigDecimal sph;
    private BigDecimal cyl;
    private BigDecimal addPower;
    private BigDecimal prism;

    private BigDecimal currentRetailPrice;
    private BigDecimal currentPurchasePrice;
    private Long currentQuantity;

    private BigDecimal salePriceAtSale;
    private BigDecimal discountPercentage;
    private BigDecimal purchasePriceAtSale;

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

    private ReportTotals totals;
    private List<ReportRow> children = new ArrayList<>();

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
