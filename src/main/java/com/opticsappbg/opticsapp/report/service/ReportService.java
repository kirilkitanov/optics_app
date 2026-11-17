
package com.opticsappbg.opticsapp.report.service;

import com.opticsappbg.opticsapp.customer.repository.CustomerRepository;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.supplier.repository.SupplierRepository;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import com.opticsappbg.opticsapp.web.dto.report.ReportResult;
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

                "products",
                productRepository.findAll().stream()
                        .sorted(Comparator
                                .comparing(
                                        (com.opticsappbg.opticsapp.product.model.Product p)
                                                -> p.getBrand(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .thenComparing(
                                        p -> p.getModel(),
                                        String.CASE_INSENSITIVE_ORDER
                                ))
                        .toList(),

                "users",
                userRepository.findAllByOrderByCreatedOnDesc()
        );
    }
}
