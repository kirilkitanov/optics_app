package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.report.model.ReportGrouping;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import com.opticsappbg.opticsapp.web.dto.report.ReportTotals;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportGroupingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter WEEK_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MM.yyyy");

    public List<ReportRow> group(List<ReportRow> rows, ReportGrouping grouping) {
        if (rows == null || rows.isEmpty()
                || grouping == null || grouping == ReportGrouping.NONE) {
            return rows == null ? new ArrayList<>() : rows;
        }

        Map<String, List<ReportRow>> grouped = new LinkedHashMap<>();
        for (ReportRow row : rows) {
            grouped.computeIfAbsent(getGroupKey(row, grouping), ignored -> new ArrayList<>())
                    .add(row);
        }

        List<ReportRow> result = new ArrayList<>();
        for (List<ReportRow> groupRows : grouped.values()) {
            ReportRow first = groupRows.get(0);
            ReportRow groupRow = new ReportRow();

            groupRow.setGroupLabel(getGroupLabel(first, grouping));
            groupRow.setTotals(calculateTotals(groupRows));
            groupRow.setChildren(new ArrayList<>(groupRows));

            if (grouping == ReportGrouping.DOCUMENT) {
                groupRow.setDate(first.getDate());
                groupRow.setDocumentId(first.getDocumentId());
                groupRow.setDocumentNumber(first.getDocumentNumber());
                groupRow.setDocumentType(first.getDocumentType());
                groupRow.setCustomerId(first.getCustomerId());
                groupRow.setCustomerName(first.getCustomerName());
                groupRow.setUsername(first.getUsername());
            } else if (grouping == ReportGrouping.CUSTOMER) {
                groupRow.setCustomerId(first.getCustomerId());
                groupRow.setCustomerName(first.getCustomerName());
            }

            result.add(groupRow);
        }

        return result;
    }

    private String getGroupKey(ReportRow row, ReportGrouping grouping) {
        return switch (grouping) {
            case DOCUMENT -> value(row.getDocumentId());
            case DAY -> row.getDate() == null ? "UNKNOWN"
                    : row.getDate().toLocalDate().toString();
            case WEEK -> row.getDate() == null ? "UNKNOWN" : weekKey(row);
            case MONTH -> row.getDate() == null ? "UNKNOWN"
                    : YearMonth.from(row.getDate()).toString();
            case YEAR -> row.getDate() == null ? "UNKNOWN"
                    : String.valueOf(row.getDate().getYear());
            case PRODUCT -> value(row.getProductId());
            case CATEGORY -> value(row.getCategoryName());
            case CUSTOMER -> value(row.getCustomerId());
            case SUPPLIER -> value(row.getSupplierName());
            case USER -> value(row.getUsername());
            case NONE -> "";
        };
    }

    private String getGroupLabel(ReportRow row, ReportGrouping grouping) {
        return switch (grouping) {
            case DOCUMENT -> value(row.getDocumentType()).isBlank()
                    ? value(row.getDocumentNumber())
                    : value(row.getDocumentType()) + " № " + value(row.getDocumentNumber());
            case DAY -> row.getDate() == null ? "Без дата"
                    : row.getDate().format(DAY_FORMAT);
            case WEEK -> row.getDate() == null ? "Без дата" : weekLabel(row);
            case MONTH -> row.getDate() == null ? "Без дата"
                    : row.getDate().format(MONTH_FORMAT);
            case YEAR -> row.getDate() == null ? "Без дата"
                    : String.valueOf(row.getDate().getYear());
            case PRODUCT -> value(row.getProductName());
            case CATEGORY -> value(row.getCategoryName());
            case CUSTOMER -> value(row.getCustomerName());
            case SUPPLIER -> value(row.getSupplierName());
            case USER -> value(row.getUsername());
            case NONE -> "";
        };
    }

    private String weekKey(ReportRow row) {
        WeekFields iso = WeekFields.ISO;
        return row.getDate().get(iso.weekBasedYear()) + "-W"
                + String.format("%02d", row.getDate().get(iso.weekOfWeekBasedYear()));
    }

    private String weekLabel(ReportRow row) {
        java.time.LocalDate start = row.getDate().toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        java.time.LocalDate end = start.plusDays(6);
        return "Седмица " + weekKey(row).substring(6)
                + " (" + start.format(WEEK_FORMAT)
                + " – " + end.format(WEEK_FORMAT) + ")";
    }

    private ReportTotals calculateTotals(List<ReportRow> rows) {
        ReportTotals totals = new ReportTotals();
        for (ReportRow row : rows) {
            totals.setQuantity(add(totals.getQuantity(), row.getQuantity()));
            totals.setPurchaseCount(add(totals.getPurchaseCount(), row.getPurchaseCount()));
            totals.setSalesAmount(add(totals.getSalesAmount(), row.getSalesAmount()));
            totals.setDeliveryAmount(add(totals.getDeliveryAmount(), row.getDeliveryAmount()));
            totals.setPurchaseValue(add(totals.getPurchaseValue(), row.getPurchaseValue()));
            totals.setProfit(add(totals.getProfit(), row.getProfit()));
            totals.setReceivedAmount(add(totals.getReceivedAmount(), row.getReceivedAmount()));
            totals.setPaidAmount(add(totals.getPaidAmount(), row.getPaidAmount()));
            totals.setRefundedAmount(add(totals.getRefundedAmount(), row.getRefundedAmount()));
            totals.setNetAmount(add(totals.getNetAmount(), row.getNetAmount()));
            totals.setIncreaseQuantity(add(totals.getIncreaseQuantity(), row.getIncreaseQuantity()));
            totals.setDecreaseQuantity(add(totals.getDecreaseQuantity(), row.getDecreaseQuantity()));
            totals.setNetDifference(add(totals.getNetDifference(), row.getNetDifference()));
            totals.setDifferencePurchaseValue(add(
                    totals.getDifferencePurchaseValue(), row.getDifferencePurchaseValue()));
            totals.setDifferenceSellingValue(add(
                    totals.getDifferenceSellingValue(), row.getDifferenceSellingValue()));
        }
        return totals;
    }

    private Long add(Long first, Long second) {
        if (first == null && second == null) return null;
        return (first == null ? 0L : first) + (second == null ? 0L : second);
    }

    private BigDecimal add(BigDecimal first, BigDecimal second) {
        if (first == null && second == null) return null;
        return (first == null ? ZERO : first).add(second == null ? ZERO : second);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
