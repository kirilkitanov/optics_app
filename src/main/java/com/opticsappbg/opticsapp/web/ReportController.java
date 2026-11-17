
package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.report.model.ReportGrouping;
import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSource;
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
        addCommonAttributes(model, user);
        model.addAttribute("filter", new ReportFilter());
        model.addAttribute("result", null);

        return "reports/index";
    }

    @PostMapping
    public String generate(
            @ModelAttribute("filter") ReportFilter filter,
            @AuthenticationPrincipal AuthenticationDetails user,
            Model model
    ) {
        addCommonAttributes(model, user);
        model.addAttribute(
                "result",
                reportService.generate(filter, canViewPurchaseValues(user))
        );

        return "reports/index";
    }

    private void addCommonAttributes(
            Model model,
            AuthenticationDetails user
    ) {
        model.addAttribute("sources", ReportSource.values());
        model.addAttribute(
                "canViewPurchaseValues",
                canViewPurchaseValues(user)
        );
        model.addAllAttributes(reportService.getFilterOptions());
        model.addAttribute("groupings", ReportGrouping.values());
        model.addAttribute("metrics", ReportMetric.values());
    }

    private boolean canViewPurchaseValues(AuthenticationDetails user) {
        return user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.OWNER;
    }
}
