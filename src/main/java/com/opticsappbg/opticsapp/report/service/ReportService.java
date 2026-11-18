
package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.supplier.repository.SupplierRepository;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import com.opticsappbg.opticsapp.web.dto.report.ReportResult;
import com.opticsappbg.opticsapp.web.dto.report.ReportProductFilterOption;
import com.opticsappbg.opticsapp.web.dto.report.ReportProductOption;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final SalesReportService salesReportService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReportResult generate(
            ReportFilter filter,
            boolean canViewPurchaseValues
    ) {
        ReportResult result = new ReportResult();
        result.setFilter(filter);
        result.setGeneratedOn(LocalDateTime.now());

        if (filter.getSources() != null
                && filter.getSources().contains(ReportSource.SALES)) {

            result.getSections().add(
                    salesReportService.generate(
                            filter,
                            canViewPurchaseValues
                    )
            );
        }

        return result;
    }

    public Map<String, Object> getFilterOptions() {
        return Map.of(
                "customers",
                customerRepository.findAll().stream()
                        .sorted(Comparator.comparing(
                                customer -> customer.getFullName(),
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .toList(),

                "suppliers",
                supplierRepository.findAllByOrderByCompanyNameAsc(),

                "users",
                userRepository.findAllByOrderByCreatedOnDesc()
        );
    }
    public List<ReportProductFilterOption> getReportProductFilters() {
        return productRepository.findDistinctReportProductFilters().stream()
                .map(row -> new ReportProductFilterOption((ProductCategory) row[0], (String) row[1], (String) row[2]))
                .toList();
    }

    public List<ReportProductOption> searchReportProducts(String query, Set<ProductCategory> categories, String brand, String model, int offset) {
        if (query == null || query.isBlank()) return List.of();
        String normalized = query.trim().toLowerCase(Locale.ROOT).replace(',', '.').replaceAll("\\s+", " ");
        String categoryCsv = categories == null || categories.isEmpty() ? "" :
                categories.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","));
        DecimalFormat format = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

        return productRepository.searchReportProducts(normalized, categoryCsv,
                        brand == null ? "" : brand, model == null ? "" : model, Math.max(0, offset))
                .stream()
                .map(p -> new ReportProductOption(p.getId(),
                        p.getBrand() + " " + p.getModel()
                                + " | S" + format.format(p.getSph())
                                + " C" + format.format(p.getCyl())
                                + " A" + format.format(p.getAddPower())
                                + " P" + format.format(p.getPrism())
                                + (p.getSizeOrDiameter() != null ? " | " + p.getSizeOrDiameter() : "")
                                + (p.getBarcode() != null ? " | " + p.getBarcode() : ""),
                        p.getCategory().name(), p.getBrand(), p.getModel()))
                .toList();
    }

    public List<Product> getProductsByIds(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : productRepository.findAllById(ids);
    }
}

