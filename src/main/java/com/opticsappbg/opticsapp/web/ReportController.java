package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.report.model.ReportSource;
import com.opticsappbg.opticsapp.report.service.SalesReportService;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.UserRole;
import com.opticsappbg.opticsapp.web.dto.report.ReportFilter;
import com.opticsappbg.opticsapp.web.dto.report.ReportSection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SalesReportService salesReportService;

    @GetMapping
    public String reports(Model model) {
        model.addAttribute("filter", new ReportFilter());
        model.addAttribute("sources", ReportSource.values());
        model.addAttribute("result", null);
        model.addAttribute("canViewPurchaseValues", false);

        return "reports/index";
    }

    @PostMapping
    public String generate(
            @ModelAttribute("filter") ReportFilter filter,
            @AuthenticationPrincipal AuthenticationDetails user,
            Model model
    ) {
        boolean canViewPurchaseValues =
                user.getRole() == UserRole.ADMIN
                        || user.getRole() == UserRole.OWNER;

        ReportSection result = null;

        if (filter.getSources() != null
                && filter.getSources().contains(ReportSource.SALES)) {
            result = salesReportService.generate(
                    filter,
                    canViewPurchaseValues
            );
        }

        model.addAttribute("sources", ReportSource.values());
        model.addAttribute("result", result);
        model.addAttribute("canViewPurchaseValues", canViewPurchaseValues);

        return "reports/index";
    }
}

