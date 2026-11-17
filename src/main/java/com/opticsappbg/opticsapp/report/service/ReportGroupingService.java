package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.report.model.ReportGrouping;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import com.opticsappbg.opticsapp.web.dto.report.ReportTotals;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ReportGroupingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MM.yyyy");

    public List<ReportRow> group(
            List<ReportRow> rows,
            ReportGrouping primary,
            ReportGrouping secondary
    ) {
        if (rows == null || rows.isEmpty()
                || primary == null || primary == ReportGrouping.NONE) {
            return rows == null ? new ArrayList<>() : rows;
        }

        return createGroups(rows, primary, secondary);
    }

    private List<ReportRow> createGroups(
            List<ReportRow> rows,
            ReportGrouping grouping,
            ReportGrouping secondary
    ) {
        Map<String, List<ReportRow>> grouped = new LinkedHashMap<>();

        for (ReportRow row : rows) {
            String key = getGroupKey(row, grouping);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(row);
        }

        List<ReportRow> result = new ArrayList<>();

        for (Map.Entry<String, List<ReportRow>> entry : grouped.entrySet()) {
            List<ReportRow> groupRows = entry.getValue();

            ReportRow groupRow = new ReportRow();
            groupRow.setGroupLabel(getGroupLabel(groupRows.get(0), grouping));
            groupRow.setTotals(calculateTotals(groupRows));

            if (secondary != null && secondary != ReportGrouping.NONE) {
                groupRow.setChildren(
                        createGroups(groupRows, secondary, ReportGrouping.NONE)
                );
            } else {
                groupRow.setChildren(new ArrayList<>(groupRows));
            }

            result.add(groupRow);
        }

        return result;
    }

    private String getGroupKey(ReportRow row, ReportGrouping grouping) {
        return switch (grouping) {
            case DOCUMENT -> value(row.getDocumentType()) + "|"
                    + value(row.getDocumentNumber());

            case DAY -> row.getDate() == null
                    ? "UNKNOWN"
                    : row.getDate().toLocalDate().toString();

            case MONTH -> row.getDate() == null
                    ? "UNKNOWN"
                    : YearMonth.from(row.getDate()).toString();

            case YEAR -> row.getDate() == null
                    ? "UNKNOWN"
                    : String.valueOf(row.getDate().getYear());

            case PRODUCT -> value(row.getProductId());

            case CATEGORY -> value(row.getCategoryName());

            case CUSTOMER -> value(row.getCustomerName());

            case SUPPLIER -> value(row.getSupplierName());

            case USER -> value(row.getUsername());

            case NONE -> "";
        };
    }

    private String getGroupLabel(ReportRow row, ReportGrouping grouping) {
        return switch (grouping) {
            case DOCUMENT -> {
                String type = value(row.getDocumentType());
                String number = value(row.getDocumentNumber());

                yield type.isBlank() ? number
                        : number.isBlank() ? type : type + " № " + number;
            }

            case DAY -> row.getDate() == null
                    ? "Без дата"
                    : row.getDate().format(DAY_FORMAT);

            case MONTH -> row.getDate() == null
                    ? "Без дата"
                    : row.getDate().format(MONTH_FORMAT);

            case YEAR -> row.getDate() == null
                    ? "Без дата"
                    : String.valueOf(row.getDate().getYear());

            case PRODUCT -> value(row.getProductName());

            case CATEGORY -> value(row.getCategoryName());

            case CUSTOMER -> value(row.getCustomerName());

            case SUPPLIER -> value(row.getSupplierName());

            case USER -> value(row.getUsername());

            case NONE -> "";
        };
    }

    private ReportTotals calculateTotals(List<ReportRow> rows) {
        ReportTotals totals = new ReportTotals();

        for (ReportRow row : rows) {
            addTotals(totals, row);
        }

        return totals;
    }

    private void addTotals(ReportTotals totals, ReportRow row) {
        totals.setQuantity(add(totals.getQuantity(), row.getQuantity()));
        totals.setPurchaseCount(add(
                totals.getPurchaseCount(), row.getPurchaseCount()
        ));

        totals.setSalesAmount(add(
                totals.getSalesAmount(), row.getSalesAmount()
        ));
        totals.setDeliveryAmount(add(
                totals.getDeliveryAmount(), row.getDeliveryAmount()
        ));
        totals.setPurchaseValue(add(
                totals.getPurchaseValue(), row.getPurchaseValue()
        ));
        totals.setProfit(add(totals.getProfit(), row.getProfit()));

        totals.setReceivedAmount(add(
                totals.getReceivedAmount(), row.getReceivedAmount()
        ));
        totals.setPaidAmount(add(
                totals.getPaidAmount(), row.getPaidAmount()
        ));
        totals.setRefundedAmount(add(
                totals.getRefundedAmount(), row.getRefundedAmount()
        ));
        totals.setNetAmount(add(
                totals.getNetAmount(), row.getNetAmount()
        ));

        totals.setIncreaseQuantity(add(
                totals.getIncreaseQuantity(), row.getIncreaseQuantity()
        ));
        totals.setDecreaseQuantity(add(
                totals.getDecreaseQuantity(), row.getDecreaseQuantity()
        ));
        totals.setNetDifference(add(
                totals.getNetDifference(), row.getNetDifference()
        ));

        totals.setDifferencePurchaseValue(add(
                totals.getDifferencePurchaseValue(),
                row.getDifferencePurchaseValue()
        ));
        totals.setDifferenceSellingValue(add(
                totals.getDifferenceSellingValue(),
                row.getDifferenceSellingValue()
        ));
    }

    private Long add(Long first, Long second) {
        if (first == null && second == null) {
            return null;
        }

        return (first == null ? 0L : first)
                + (second == null ? 0L : second);
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        if (first == null && second == null) {
            return null;
        }

        return (first == null ? ZERO : first)
                .add(second == null ? ZERO : second);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}

