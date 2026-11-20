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
                groupRow.setSupplierName(first.getSupplierName());
                groupRow.setUsername(first.getUsername());
            } else if (grouping == ReportGrouping.CUSTOMER) {
                groupRow.setCustomerId(first.getCustomerId());
                groupRow.setCustomerName(first.getCustomerName());
            } else if (grouping == ReportGrouping.SUPPLIER) {
                groupRow.setSupplierName(first.getSupplierName());
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
            case PRODUCT -> productGroupKey(row);
            case CATEGORY -> hasValue(row.getCategoryName()) ? value(row.getCategoryName()) : "NO_CATEGORY";
            case CUSTOMER -> row.getCustomerId() == null ? "NO_CUSTOMER" : value(row.getCustomerId());
            case SUPPLIER -> hasValue(row.getSupplierName()) ? value(row.getSupplierName()) : "NO_SUPPLIER";
            case USER -> hasValue(row.getUsername()) ? value(row.getUsername()) : "NO_USER";
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
            case PRODUCT -> productGroupLabel(row);
            case CATEGORY -> hasValue(row.getCategoryName()) ? value(row.getCategoryName()) : "Без категория";
            case CUSTOMER -> hasValue(row.getCustomerName()) ? value(row.getCustomerName()) : "Без клиент";
            case SUPPLIER -> hasValue(row.getSupplierName()) ? value(row.getSupplierName()) : "Без доставчик";
            case USER -> hasValue(row.getUsername()) ? value(row.getUsername()) : "Без потребител";
            case NONE -> "";
        };
    }


    private String productGroupKey(ReportRow row) {
        return String.join("|",
                normalizeKey(row.getCategoryName()),
                normalizeKey(row.getProductName()),
                isOpticalProduct(row) ? normalizeDecimal(row.getSph()) : "",
                isOpticalProduct(row) ? normalizeDecimal(row.getCyl()) : "",
                isOpticalProduct(row) ? normalizeDecimal(row.getAddPower()) : "",
                isOpticalProduct(row) ? normalizeDecimal(row.getPrism()) : "");
    }

    private String productGroupLabel(ReportRow row) {
        String category = hasValue(row.getCategoryName())
                ? value(row.getCategoryName()) : "Без категория";
        String product = hasValue(row.getProductName())
                ? value(row.getProductName()) : "Без продукт";
        String label = category + " | " + product;

        if (isOpticalProduct(row)) {
            label += " | S" + displayDecimal(row.getSph())
                    + " C" + displayDecimal(row.getCyl())
                    + " A" + displayDecimal(row.getAddPower())
                    + " P" + displayDecimal(row.getPrism());
        }
        return label;
    }

    private boolean isOpticalProduct(ReportRow row) {
        return "Диоптрично стъкло".equals(row.getCategoryName())
                || "Контактна леща".equals(row.getCategoryName());
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String displayDecimal(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
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

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
