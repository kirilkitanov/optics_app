package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import com.opticsappbg.opticsapp.delivery.model.DeliveryDocumentType;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.report.model.ReportGrouping;
import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.report.model.ReportSortDirection;
import com.opticsappbg.opticsapp.report.model.ReportSortField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class ReportFilter {

    private Set<ReportSource> sources = new LinkedHashSet<>();

    private LocalDate fromDate;
    private LocalDate toDate;

    private Set<Long> productIds = new LinkedHashSet<>();
    private Set<ProductCategory> categories = new LinkedHashSet<>();
    private String brand;
    private String model;
    private String barcode;

    private Long customerId;
    private Long supplierId;
    private Long userId;

    private Set<OrderStatus> orderStatuses = new LinkedHashSet<>();
    private Set<DeliveryDocumentType> deliveryDocumentTypes = new LinkedHashSet<>();
    private Set<PaymentMethod> paymentMethods = new LinkedHashSet<>();

    private Set<ReportMetric> metrics = new LinkedHashSet<>();

    private ReportGrouping primaryGrouping = ReportGrouping.NONE;

    private ReportSortField sortBy = ReportSortField.DATE;
    private ReportSortDirection sortDirection = ReportSortDirection.DESC;
}