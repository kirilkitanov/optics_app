package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSortDirection;
import com.opticsappbg.opticsapp.report.model.ReportSortField;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportSortingService {

    public void sort(
            List<ReportRow> rows,
            ReportSortField sortBy,
            ReportSortDirection direction,
            ReportMetric sortMetric
    ) {
        if (rows == null || rows.size() < 2) {
            return;
        }

        ReportSortField field = sortBy != null
                ? sortBy : ReportSortField.DATE;

        boolean ascending = direction == ReportSortDirection.ASC;

        Comparator<ReportRow> comparator = (first, second) ->
                compareValues(getValue(first, field, sortMetric),
                        getValue(second, field, sortMetric));

        if (!ascending) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    private Object getValue(
            ReportRow row,
            ReportSortField field,
            ReportMetric sortMetric
    ) {
        if (row == null) {
            return null;
        }

        if (field == ReportSortField.METRIC) {
            Object value = row.getTotals() != null && sortMetric != null
                    ? row.getTotals().getMetricValue(sortMetric)
                    : sortMetric == null ? null : row.getMetricValue(sortMetric);

            return toBigDecimal(value);
        }

        return switch (field) {
            case DATE -> row.getDate();
            case DOCUMENT -> text(row.getDocumentType()) + " "
                    + text(row.getDocumentNumber());
            case PRODUCT -> text(row.getProductName());
            case CATEGORY -> text(row.getCategoryName());
            case CUSTOMER -> text(row.getCustomerName());
            case SUPPLIER -> text(row.getSupplierName());
            case USER -> text(row.getUsername());
            case METRIC -> null;
        };
    }

    private int compareValues(Object first, Object second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }

        if (first instanceof Comparable<?> && first.getClass().isInstance(second)) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparable = (Comparable<Object>) first;
            return comparable.compareTo(second);
        }

        return first.toString().compareToIgnoreCase(second.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
