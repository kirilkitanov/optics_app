package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
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

    private String customerName;
    private String supplierName;
    private String username;
    private String reason;
    private String groupLabel;

    private PaymentMethod paymentMethod;

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

    private ReportTotals totals;
    private List<ReportRow> children = new ArrayList<>();
}
