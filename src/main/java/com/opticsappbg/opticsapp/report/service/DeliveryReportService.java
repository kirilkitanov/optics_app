package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.model.DeliveryDocumentType;
import com.opticsappbg.opticsapp.delivery.model.DeliveryItem;
import com.opticsappbg.opticsapp.delivery.repository.DeliveryRepository;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.report.model.ReportGrouping;
import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.supplier.model.SupplierPayment;
import com.opticsappbg.opticsapp.supplier.repository.SupplierPaymentRepository;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import com.opticsappbg.opticsapp.web.dto.report.ReportSection;
import com.opticsappbg.opticsapp.web.dto.report.ReportTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DeliveryReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int SCALE = 2;

    private final DeliveryRepository deliveryRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ReportGroupingService groupingService;
    private final ReportSortingService sortingService;

    public DeliveryReportService(
            DeliveryRepository deliveryRepository,
            SupplierPaymentRepository supplierPaymentRepository,
            ReportGroupingService groupingService,
            ReportSortingService sortingService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.groupingService = groupingService;
        this.sortingService = sortingService;
    }

    public ReportSection generate(ReportFilter filter, boolean canViewPurchaseValues) {
        Objects.requireNonNull(filter, "Филтърът не може да бъде null.");

        ReportSection section = new ReportSection();
        section.setSource(ReportSource.DELIVERIES);
        section.setTitle(ReportSource.DELIVERIES.getDisplayName());

        Set<ReportMetric> metrics = resolveMetrics(filter, canViewPurchaseValues);
        section.setColumns(new ArrayList<>(metrics));

        List<Delivery> deliveries = findDeliveries(filter);
        List<Long> deliveryIds = deliveries.stream().map(Delivery::getId).toList();

        Map<Long, List<SupplierPayment>> paymentsByDelivery = new HashMap<>();
        if (!deliveryIds.isEmpty()) {
            for (SupplierPayment payment : supplierPaymentRepository
                    .findByDelivery_IdInOrderByPaymentDateAsc(deliveryIds)) {
                paymentsByDelivery.computeIfAbsent(
                        payment.getDelivery().getId(), ignored -> new ArrayList<>()
                ).add(payment);
            }
        }

        Set<PaymentMethod> selectedMethods = filter.getPaymentMethods() == null
                ? Set.of() : filter.getPaymentMethods();

        List<ReportRow> rows = new ArrayList<>();
        ReportTotals totals = new ReportTotals();
        Map<Long, Delivery> eligibleDeliveries = new LinkedHashMap<>();

        for (Delivery delivery : deliveries) {
            List<SupplierPayment> payments = paymentsByDelivery.getOrDefault(
                    delivery.getId(), List.of()
            );

            if (!selectedMethods.isEmpty()
                    && payments.stream().noneMatch(p -> selectedMethods.contains(p.getPaymentMethod()))) {
                continue;
            }

            boolean hasMatchingItem = false;
            Set<Long> countedProducts = new HashSet<>();
            Map<Long, BigDecimal> allocatedAmounts = allocateDeliveryAmounts(delivery);
            for (DeliveryItem item : delivery.getItems()) {
                Product product = item.getProduct();
                if (!matchesProductFilters(product, filter)) {
                    continue;
                }

                hasMatchingItem = true;
                ReportRow row = createRow(
                        delivery, item, product,
                        allocatedAmounts.getOrDefault(item.getId(), ZERO),
                        countedProducts.add(product.getId()), canViewPurchaseValues
                );
                rows.add(row);
                addToTotals(totals, row, canViewPurchaseValues);
            }

            if (hasMatchingItem) {
                eligibleDeliveries.put(delivery.getId(), delivery);
            }
        }

        sortingService.sort(rows, filter.getSortBy(), filter.getSortDirection());

        ReportGrouping grouping = filter.getPrimaryGrouping();
        List<ReportRow> groupedRows = groupingService.group(rows, grouping);

        if (grouping == ReportGrouping.DOCUMENT
                || grouping == ReportGrouping.SUPPLIER
                || grouping == ReportGrouping.DAY
                || grouping == ReportGrouping.WEEK
                || grouping == ReportGrouping.MONTH
                || grouping == ReportGrouping.YEAR) {
            applyPaymentBalances(
                    groupedRows, eligibleDeliveries, paymentsByDelivery, grouping, totals
            );
        }

        section.setRows(groupedRows);
        section.setTotals(totals);
        return section;
    }

    private List<Delivery> findDeliveries(ReportFilter filter) {
        LocalDateTime fromDate = filter.getFromDate() == null
                ? null : filter.getFromDate().atStartOfDay();
        LocalDateTime toDate = filter.getToDate() == null
                ? null : filter.getToDate().plusDays(1).atStartOfDay();

        Set<DeliveryDocumentType> types = filter.getDeliveryDocumentTypes() == null
                ? Set.of() : filter.getDeliveryDocumentTypes();

        return deliveryRepository.findAllForReport(fromDate, toDate).stream()
                .filter(d -> filter.getSupplierId() == null
                        || (d.getSupplier() != null
                        && filter.getSupplierId().equals(d.getSupplier().getId())))
                .filter(d -> filter.getUserId() == null
                        || (d.getCreatedBy() != null
                        && filter.getUserId().equals(d.getCreatedBy().getId())))
                .filter(d -> types.isEmpty() || types.contains(d.getDocumentType()))
                .toList();
    }

    private boolean matchesProductFilters(Product product, ReportFilter filter) {
        if (product == null) return false;

        if (filter.getProductIds() != null && !filter.getProductIds().isEmpty()
                && !filter.getProductIds().contains(product.getId())) return false;

        if (filter.getCategories() != null && !filter.getCategories().isEmpty()
                && !filter.getCategories().contains(product.getCategory())) return false;

        if (hasText(filter.getBrand()) && !containsIgnoreCase(product.getBrand(), filter.getBrand())) {
            return false;
        }
        if (hasText(filter.getModel()) && !containsIgnoreCase(product.getModel(), filter.getModel())) {
            return false;
        }
        return !hasText(filter.getBarcode())
                || containsIgnoreCase(product.getBarcode(), filter.getBarcode());
    }

    private ReportRow createRow(
            Delivery delivery,
            DeliveryItem item,
            Product product,
            BigDecimal lineAmount,
            boolean firstOccurrenceOfProduct,
            boolean canViewPurchaseValues
    ) {
        ReportRow row = new ReportRow();
        row.setDate(delivery.getInvoiceDate());
        row.setDocumentId(delivery.getId());
        row.setDocumentNumber(delivery.getInvoiceNumber());
        row.setDocumentType(delivery.getDocumentType() == null
                ? null : delivery.getDocumentType().getDisplayName());
        row.setSupplierName(delivery.getSupplier() == null
                ? null : delivery.getSupplier().getCompanyName());
        row.setUsername(delivery.getCreatedBy() == null
                ? null : delivery.getCreatedBy().getFullName());

        row.setProductId(product.getId());
        row.setProductName(buildProductName(product));
        row.setCategoryName(product.getCategory() == null
                ? null : product.getCategory().getDisplayName());
        long quantity = item.getQuantity() == null ? 0L : item.getQuantity().longValue();
        row.setQuantity(delivery.getDocumentType() == DeliveryDocumentType.CREDIT_NOTE
                ? -quantity : quantity);
        row.setPurchaseCount(firstOccurrenceOfProduct ? 1L : 0L);
        row.setSph(product.getSph());
        row.setCyl(product.getCyl());
        row.setAddPower(product.getAddPower());
        row.setPrism(product.getPrism());
        row.setCurrentQuantity(product.getQuantity() == null ? 0L : product.getQuantity().longValue());

        row.setDeliveryAmount(round(lineAmount));

        if (canViewPurchaseValues) {
            row.setCurrentPurchasePrice(product.getPurchasePrice());
            row.setPurchasePriceAtSale(item.getPurchasePriceAtDelivery());
            row.setPurchaseValue(round(lineAmount));
        }

        return row;
    }

    private Map<Long, BigDecimal> allocateDeliveryAmounts(Delivery delivery) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        List<DeliveryItem> items = delivery.getItems();

        if (items == null || items.isEmpty()) return result;

        BigDecimal totalWeight = ZERO;
        for (DeliveryItem item : items) {
            if (item.getPurchasePriceAtDelivery() != null && item.getQuantity() != null) {
                totalWeight = totalWeight.add(
                        item.getPurchasePriceAtDelivery()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            }
        }

        BigDecimal documentTotal = delivery.getTotalAmount() == null
                ? ZERO : delivery.getTotalAmount();
        BigDecimal allocated = ZERO;

        for (int i = 0; i < items.size(); i++) {
            DeliveryItem item = items.get(i);
            BigDecimal amount;

            if (i == items.size() - 1) {
                amount = documentTotal.subtract(allocated);
            } else if (totalWeight.compareTo(ZERO) > 0
                    && item.getPurchasePriceAtDelivery() != null
                    && item.getQuantity() != null) {
                BigDecimal weight = item.getPurchasePriceAtDelivery()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                amount = weight.multiply(documentTotal)
                        .divide(totalWeight, SCALE, RoundingMode.HALF_UP);
                allocated = allocated.add(amount);
            } else {
                amount = ZERO.setScale(SCALE, RoundingMode.HALF_UP);
            }

            result.put(item.getId(), amount);
        }

        return result;
    }

    private void applyPaymentBalances(
            List<ReportRow> groupedRows,
            Map<Long, Delivery> eligibleDeliveries,
            Map<Long, List<SupplierPayment>> paymentsByDelivery,
            ReportGrouping grouping,
            ReportTotals sectionTotals
    ) {
        if (eligibleDeliveries.isEmpty()) return;

        BigDecimal allDocumentAmount = ZERO;
        BigDecimal allPaid = ZERO;

        for (Delivery delivery : eligibleDeliveries.values()) {
            BigDecimal amount = delivery.getTotalAmount() == null ? ZERO : delivery.getTotalAmount();
            BigDecimal paid = sumPayments(paymentsByDelivery.getOrDefault(delivery.getId(), List.of()));
            allDocumentAmount = allDocumentAmount.add(amount);
            allPaid = allPaid.add(paid);
        }

        sectionTotals.setPurchaseCount((long) eligibleDeliveries.size());
        sectionTotals.setDocumentAmount(round(allDocumentAmount));
        sectionTotals.setNetAmount(round(allPaid));
        sectionTotals.setPaidAmount(round(allPaid));
        sectionTotals.setOutstandingAmount(round(allDocumentAmount.subtract(allPaid)));
        sectionTotals.setPaymentStatus(resolvePaymentStatus(allDocumentAmount, allPaid));

        for (ReportRow group : groupedRows) {
            Set<Long> ids = new LinkedHashSet<>();
            if (grouping == ReportGrouping.DOCUMENT) {
                if (group.getDocumentId() != null) ids.add(group.getDocumentId());
            } else {
                for (ReportRow child : group.getChildren()) {
                    if (child.getDocumentId() != null) ids.add(child.getDocumentId());
                }
            }

            BigDecimal documentAmount = ZERO;
            BigDecimal paid = ZERO;
            Set<String> methods = new LinkedHashSet<>();

            for (Long id : ids) {
                Delivery delivery = eligibleDeliveries.get(id);
                if (delivery == null) continue;

                documentAmount = documentAmount.add(
                        delivery.getTotalAmount() == null ? ZERO : delivery.getTotalAmount()
                );

                for (SupplierPayment payment : paymentsByDelivery.getOrDefault(id, List.of())) {
                    if (payment.getAmount() != null) paid = paid.add(payment.getAmount());
                    if (payment.getPaymentMethod() != null) {
                        methods.add(payment.getPaymentMethod().getDisplayName());
                    }
                }
            }

            group.setDocumentAmount(round(documentAmount));
            group.setNetAmount(round(paid));
            group.setPaidAmount(round(paid));
            group.setOutstandingAmount(round(documentAmount.subtract(paid)));
            group.setPaymentStatus(resolvePaymentStatus(documentAmount, paid));

            if (grouping == ReportGrouping.DOCUMENT) {
                group.setPaymentMethodsSummary(methods.size() == 1
                        ? methods.iterator().next() : null);
            }

            if (group.getTotals() != null) {
                group.getTotals().setPurchaseCount((long) ids.size());
            }
        }
    }

    private BigDecimal sumPayments(List<SupplierPayment> payments) {
        return payments.stream()
                .map(SupplierPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);
    }

    private String resolvePaymentStatus(BigDecimal documentAmount, BigDecimal paid) {
        BigDecimal due = documentAmount.abs();
        BigDecimal settled = paid.abs();

        if (due.compareTo(ZERO) == 0) {
            return settled.compareTo(ZERO) > 0 ? "Надплатена" : "Платена";
        }
        if (settled.compareTo(ZERO) == 0) return "Неплатена";
        if (settled.compareTo(due) > 0) return "Надплатена";
        if (settled.compareTo(due) == 0) return "Платена";
        return "Частично платена";
    }

    private void addToTotals(ReportTotals totals, ReportRow row, boolean canViewPurchaseValues) {
        totals.setQuantity(add(totals.getQuantity(), row.getQuantity()));
        totals.setDeliveryAmount(add(totals.getDeliveryAmount(), row.getDeliveryAmount()));
        if (canViewPurchaseValues && row.getPurchaseValue() != null) {
            totals.setPurchaseValue(add(totals.getPurchaseValue(), row.getPurchaseValue()));
        }
    }

    private Set<ReportMetric> resolveMetrics(ReportFilter filter, boolean canViewPurchaseValues) {
        Set<ReportMetric> metrics = new LinkedHashSet<>();
        if (filter.getMetrics() == null || filter.getMetrics().isEmpty()) {
            metrics.add(ReportMetric.QUANTITY);
            metrics.add(ReportMetric.PURCHASE_COUNT);
            metrics.add(ReportMetric.DELIVERY_AMOUNT);
        } else {
            metrics.addAll(filter.getMetrics());
        }
        if (!canViewPurchaseValues) {
            metrics.remove(ReportMetric.DELIVERY_AMOUNT);
            metrics.remove(ReportMetric.PURCHASE_VALUE);
            metrics.remove(ReportMetric.PROFIT);
            metrics.remove(ReportMetric.DIFFERENCE_PURCHASE_VALUE);
        }
        return metrics;
    }

    private String buildProductName(Product product) {
        String brand = product.getBrand() == null ? "" : product.getBrand().trim();
        String model = product.getModel() == null ? "" : product.getModel().trim();
        return (brand + " " + model).trim();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && search != null
                && value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        if (first == null && second == null) return null;
        return (first == null ? ZERO : first).add(second == null ? ZERO : second);
    }

    private Long add(Long first, Long second) {
        if (first == null && second == null) return null;
        return (first == null ? 0L : first) + (second == null ? 0L : second);
    }
}