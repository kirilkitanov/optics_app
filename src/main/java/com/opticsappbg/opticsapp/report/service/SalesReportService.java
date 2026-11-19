package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.customer.model.CustomerPayment;
import com.opticsappbg.opticsapp.customer.repository.CustomerPaymentRepository;
import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderItem;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.order.repository.OrderRepository;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import com.opticsappbg.opticsapp.web.dto.report.ReportSection;
import com.opticsappbg.opticsapp.web.dto.report.ReportTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SalesReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int SCALE = 2;
    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final OrderRepository orderRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final ReportGroupingService groupingService;
    private final ReportSortingService sortingService;

    public SalesReportService(OrderRepository orderRepository,
                              CustomerPaymentRepository customerPaymentRepository,
                              ReportGroupingService groupingService,
                              ReportSortingService sortingService) {
        this.orderRepository = orderRepository;
        this.customerPaymentRepository = customerPaymentRepository;
        this.groupingService = groupingService;
        this.sortingService = sortingService;
    }

    public ReportSection generate(
            ReportFilter filter,
            boolean canViewPurchaseValues
    ) {
        Objects.requireNonNull(filter, "Филтърът не може да бъде null.");

        ReportSection section = new ReportSection();
        section.setSource(ReportSource.SALES);
        section.setTitle(ReportSource.SALES.getDisplayName());

        Set<ReportMetric> metrics = resolveMetrics(filter, canViewPurchaseValues);
        section.setColumns(new ArrayList<>(metrics));

        List<Order> orders = findOrders(filter);

        ReportTotals totals = new ReportTotals();
        List<ReportRow> rows = new ArrayList<>();

        for (Order order : orders) {
            Map<Long, BigDecimal> allocatedSales = allocateSalesAmounts(order);

            Set<Long> countedProducts = new HashSet<>();

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();

                if (!matchesProductFilters(product, filter)) {
                    continue;
                }

                ReportRow row = createRow(
                        order,
                        item,
                        product,
                        allocatedSales.getOrDefault(item.getId(), ZERO),
                        countedProducts.add(product.getId()),
                        canViewPurchaseValues
                );

                rows.add(row);
                addToTotals(totals, row, canViewPurchaseValues);
            }
        }

        sortingService.sort(
                rows,
                filter.getSortBy(),
                filter.getSortDirection()
        );

        List<ReportRow> groupedRows = groupingService.group(
                rows,
                filter.getPrimaryGrouping()
        );

        if (filter.getPrimaryGrouping() == com.opticsappbg.opticsapp.report.model.ReportGrouping.DOCUMENT) {
            addDocumentPayments(groupedRows);
        }

        section.setRows(groupedRows);
        section.setTotals(totals);

        return section;
    }

    private void addDocumentPayments(List<ReportRow> groupedRows) {
        List<Long> orderIds = groupedRows.stream()
                .map(ReportRow::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (orderIds.isEmpty()) {
            return;
        }

        Map<Long, List<CustomerPayment>> paymentsByOrder = new HashMap<>();
        for (CustomerPayment payment : customerPaymentRepository
                .findByOrder_IdInOrderByPaymentDateAsc(orderIds)) {
            Long orderId = payment.getOrder().getId();
            paymentsByOrder.computeIfAbsent(orderId, ignored -> new ArrayList<>())
                    .add(payment);
        }

        for (ReportRow group : groupedRows) {
            List<CustomerPayment> payments = paymentsByOrder.get(group.getDocumentId());
            if (payments == null || payments.isEmpty()) {
                continue;
            }

            List<String> paymentLines = new ArrayList<>();
            Set<String> methods = new LinkedHashSet<>();

            for (CustomerPayment payment : payments) {
                String date = payment.getPaymentDate() != null
                        ? payment.getPaymentDate().format(PAYMENT_DATE_FORMAT)
                        : "Без дата";
                String amount = payment.getAmount() != null
                        ? payment.getAmount().setScale(SCALE, RoundingMode.HALF_UP).toPlainString()
                        : "0.00";

                paymentLines.add(date + "  " + amount);
                if (payment.getPaymentMethod() != null) {
                    methods.add(payment.getPaymentMethod().getDisplayName());
                }
            }

            group.setPaymentsSummary(String.join("\n", paymentLines));
            group.setPaymentMethodsSummary(
                    methods.isEmpty() ? null : String.join(", ", methods)
            );
        }
    }

    private List<Order> findOrders(ReportFilter filter) {
        LocalDateTime fromDate = filter.getFromDate() != null
                ? filter.getFromDate().atStartOfDay()
                : null;

        LocalDateTime toDate = filter.getToDate() != null
                ? filter.getToDate().plusDays(1).atStartOfDay()
                : null;

        Set<OrderStatus> statuses = filter.getOrderStatuses() == null
                ? Set.of()
                : filter.getOrderStatuses();

        Set<Long> productIds = filter.getProductIds() == null
                ? Set.of()
                : filter.getProductIds();

        Set<ProductCategory> categories = filter.getCategories() == null
                ? Set.of()
                : filter.getCategories();

        boolean filterProducts =
                !productIds.isEmpty()
                        || !categories.isEmpty()
                        || hasText(filter.getBrand())
                        || hasText(filter.getModel())
                        || hasText(filter.getBarcode());

        return orderRepository.findSalesForReport(
                OrderStatus.CANCELLED,
                fromDate,
                toDate,
                filter.getCustomerId(),
                filter.getUserId(),
                !statuses.isEmpty(),
                statuses.isEmpty()
                        ? Arrays.asList(OrderStatus.values())
                        : new ArrayList<>(statuses),
                filterProducts,
                !productIds.isEmpty(),
                productIds.isEmpty() ? List.of(-1L) : new ArrayList<>(productIds),
                !categories.isEmpty(),
                categories.isEmpty()
                        ? Arrays.asList(ProductCategory.values())
                        : new ArrayList<>(categories),
                normalize(filter.getBrand()),
                normalize(filter.getModel()),
                normalize(filter.getBarcode())
        );
    }

    private boolean matchesProductFilters(Product product, ReportFilter filter) {
        if (product == null) {
            return false;
        }

        if (filter.getProductIds() != null
                && !filter.getProductIds().isEmpty()
                && !filter.getProductIds().contains(product.getId())) {
            return false;
        }

        if (filter.getCategories() != null
                && !filter.getCategories().isEmpty()
                && !filter.getCategories().contains(product.getCategory())) {
            return false;
        }

        if (hasText(filter.getBrand())
                && !containsIgnoreCase(product.getBrand(), filter.getBrand())) {
            return false;
        }

        if (hasText(filter.getModel())
                && !containsIgnoreCase(product.getModel(), filter.getModel())) {
            return false;
        }

        return !hasText(filter.getBarcode())
                || containsIgnoreCase(product.getBarcode(), filter.getBarcode());
    }

    private ReportRow createRow(
            Order order,
            OrderItem item,
            Product product,
            BigDecimal salesAmount,
            boolean firstPurchaseOfProduct,
            boolean canViewPurchaseValues
    ) {
        ReportRow row = new ReportRow();

        row.setDate(order.getCreatedOn());
        row.setDocumentId(order.getId());
        row.setDocumentNumber(order.getOrderNumber());
        row.setDocumentType("Продажба");

        row.setProductId(product.getId());
        row.setProductName(buildProductName(product));

        row.setCategoryName(product.getCategory() != null
                ? product.getCategory().getDisplayName()
                : null);

        row.setCustomerName(order.getCustomer() != null
                ? order.getCustomer().getFullName()
                : null);

        row.setUsername(order.getCreatedBy() != null
                ? order.getCreatedBy().getFullName()
                : null);

        row.setQuantity(item.getQuantity() != null
                ? item.getQuantity().longValue()
                : 0L);

        row.setSph(product.getSph());
        row.setCyl(product.getCyl());
        row.setAddPower(product.getAddPower());
        row.setPrism(product.getPrism());

        row.setCurrentRetailPrice(product.getRetailPrice());
        row.setCurrentQuantity(product.getQuantity());
        row.setSalePriceAtSale(item.getSalePriceAtSale());

        row.setPurchaseCount(firstPurchaseOfProduct ? 1L : 0L);
        row.setSalesAmount(salesAmount);

        if (canViewPurchaseValues) {
            row.setCurrentPurchasePrice(product.getPurchasePrice());
            row.setPurchasePriceAtSale(item.getPurchasePriceAtSale());

            if (item.getPurchasePriceAtSale() != null) {
                row.setPurchaseValue(
                        item.getPurchasePriceAtSale()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                                .setScale(SCALE, RoundingMode.HALF_UP)
                );
            }

            row.setProfit(item.getProfitAtSale());
        }

        return row;
    }

    /*
     * Изчислява стойността на всеки ред така, че да използва
     * записаната историческа печалба, когато има такава.
     *
     * За редове без достатъчно исторически данни остатъкът от
     * крайната сума се разпределя пропорционално на брутната стойност.
     */
    private Map<Long, BigDecimal> allocateSalesAmounts(Order order) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();

        List<OrderItem> items = order.getItems();

        if (items == null || items.isEmpty()) {
            return result;
        }

        BigDecimal allocated = ZERO;
        BigDecimal remainingWeight = ZERO;

        List<OrderItem> itemsNeedingAllocation = new ArrayList<>();

        for (OrderItem item : items) {
            BigDecimal gross = getGrossAmount(item);

            if (item.getPurchasePriceAtSale() != null
                    && item.getProfitAtSale() != null) {

                BigDecimal cost = item.getPurchasePriceAtSale()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                BigDecimal historicalSales = item.getProfitAtSale().add(cost)
                        .setScale(SCALE, RoundingMode.HALF_UP);

                result.put(item.getId(), historicalSales);
                allocated = allocated.add(historicalSales);
            } else {
                itemsNeedingAllocation.add(item);
                remainingWeight = remainingWeight.add(gross);
            }
        }

        BigDecimal orderTotal = order.getTotal() != null
                ? order.getTotal()
                : ZERO;

        BigDecimal remainingAmount = orderTotal.subtract(allocated);

        BigDecimal distributed = ZERO;

        for (int i = 0; i < itemsNeedingAllocation.size(); i++) {
            OrderItem item = itemsNeedingAllocation.get(i);
            BigDecimal amount;

            if (i == itemsNeedingAllocation.size() - 1) {
                amount = remainingAmount.subtract(distributed);
            } else if (remainingWeight.compareTo(ZERO) > 0) {
                amount = getGrossAmount(item)
                        .multiply(remainingAmount)
                        .divide(remainingWeight, SCALE, RoundingMode.HALF_UP);

                distributed = distributed.add(amount);
            } else {
                amount = ZERO;
            }

            result.put(item.getId(), amount);
        }

        return result;
    }

    private BigDecimal getGrossAmount(OrderItem item) {
        if (item.getSalePriceAtSale() == null || item.getQuantity() == null) {
            return ZERO;
        }

        return item.getSalePriceAtSale()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private void addToTotals(
            ReportTotals totals,
            ReportRow row,
            boolean canViewPurchaseValues
    ) {
        totals.setQuantity(add(totals.getQuantity(), row.getQuantity()));
        totals.setPurchaseCount(add(
                totals.getPurchaseCount(),
                row.getPurchaseCount()
        ));

        totals.setSalesAmount(add(
                totals.getSalesAmount(),
                row.getSalesAmount()
        ));

        if (canViewPurchaseValues) {
            if (row.getPurchaseValue() != null) {
                totals.setPurchaseValue(add(
                        totals.getPurchaseValue(),
                        row.getPurchaseValue()
                ));
            }

            if (row.getProfit() != null) {
                totals.setProfit(add(
                        totals.getProfit(),
                        row.getProfit()
                ));
            }
        }
    }

    private Set<ReportMetric> resolveMetrics(
            ReportFilter filter,
            boolean canViewPurchaseValues
    ) {
        Set<ReportMetric> metrics = new LinkedHashSet<>();

        if (filter.getMetrics() == null || filter.getMetrics().isEmpty()) {
            metrics.add(ReportMetric.QUANTITY);
            metrics.add(ReportMetric.PURCHASE_COUNT);
            metrics.add(ReportMetric.SALES_AMOUNT);

            if (canViewPurchaseValues) {
                metrics.add(ReportMetric.PURCHASE_VALUE);
                metrics.add(ReportMetric.PROFIT);
            }
        } else {
            metrics.addAll(filter.getMetrics());
        }

        if (!canViewPurchaseValues) {
            metrics.remove(ReportMetric.PURCHASE_VALUE);
            metrics.remove(ReportMetric.PROFIT);
            metrics.remove(ReportMetric.DELIVERY_AMOUNT);
            metrics.remove(ReportMetric.DIFFERENCE_PURCHASE_VALUE);
        }

        return metrics;
    }

    private String buildProductName(Product product) {
        String brand = product.getBrand() != null
                ? product.getBrand().trim()
                : "";

        String model = product.getModel() != null
                ? product.getModel().trim()
                : "";

        return (brand + " " + model).trim();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private Long add(Long first, Long second) {
        return (first == null ? 0L : first)
                + (second == null ? 0L : second);
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        return (first == null ? ZERO : first)
                .add(second == null ? ZERO : second)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }
}
