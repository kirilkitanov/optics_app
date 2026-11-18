
package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.report.model.ReportSortDirection;
import com.opticsappbg.opticsapp.report.model.ReportSortField;
import com.opticsappbg.opticsapp.web.dto.report.ReportRow;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ReportSortingService {

    public void sort(
            List<ReportRow> rows,
            ReportSortField sortBy,
            ReportSortDirection direction
    ) {
        if (rows == null || rows.size() < 2) {
            return;
        }

        ReportSortField field = sortBy != null
                ? sortBy : ReportSortField.DATE;

        Comparator<ReportRow> comparator = (first, second) ->
                compareValues(getValue(first, field), getValue(second, field));

        if (direction != ReportSortDirection.ASC) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    private Object getValue(ReportRow row, ReportSortField field) {
        if (row == null) {
            return null;
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

    private String text(String value) {
        return value == null ? "" : value;
    }
}
