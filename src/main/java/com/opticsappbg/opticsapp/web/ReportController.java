
package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.report.model.*;
import com.opticsappbg.opticsapp.report.service.ReportService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.UserRole;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.web.dto.report.ReportProductFilterOption;
import com.opticsappbg.opticsapp.web.dto.report.ReportProductOption;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String reports(
            @AuthenticationPrincipal AuthenticationDetails user,
            Model model
    ) {
        ReportFilter filter = new ReportFilter();
        addCommonAttributes(model, user, filter);
        model.addAttribute("filter", filter);
        model.addAttribute("result", null);

        return "reports/reports";
    }

    @PostMapping
    public String generate(
            @ModelAttribute("filter") ReportFilter filter,
            @AuthenticationPrincipal AuthenticationDetails user,
            Model model
    ) {
        addCommonAttributes(model, user, filter);
        model.addAttribute(
                "result",
                reportService.generate(filter, canViewPurchaseValues(user))
        );

        return "reports/reports";
    }


    @GetMapping("/products/filters")
    @ResponseBody
    public List<ReportProductFilterOption> productFilters() {
        return reportService.getReportProductFilters();
    }

    @GetMapping("/products/search")
    @ResponseBody
    public List<ReportProductOption> searchProducts(
            @RequestParam String query,
            @RequestParam(required = false) Set<ProductCategory> categories,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return reportService.searchReportProducts(query, categories, brand, model, offset);
    }

    private void addCommonAttributes(
            Model model,
            AuthenticationDetails user,
            ReportFilter filter
    ) {
        model.addAttribute("selectedProducts", reportService.getProductsByIds(filter.getProductIds()));
        model.addAttribute("sources", ReportSource.values());
        model.addAttribute(
                "canViewPurchaseValues",
                canViewPurchaseValues(user)
        );
        model.addAllAttributes(reportService.getFilterOptions());
        model.addAttribute("groupings", ReportGrouping.values());
        model.addAttribute("sortFields", ReportSortField.values());
        model.addAttribute("sortDirections", ReportSortDirection.values());
    }

    private boolean canViewPurchaseValues(AuthenticationDetails user) {
        return user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.OWNER;
    }
}
