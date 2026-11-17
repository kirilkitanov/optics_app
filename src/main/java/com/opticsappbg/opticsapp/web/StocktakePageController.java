package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.stocktake.service.ExcelStocktakeExportService;
import com.opticsappbg.opticsapp.stocktake.service.StocktakeService;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.model.UserRole;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.ProductListItemDto;
import com.opticsappbg.opticsapp.web.dto.StocktakeImportPreviewDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/stocktakes")
@RequiredArgsConstructor
public class StocktakePageController {

    private final ProductService productService;
    private final UserService userService;
    private final StocktakeService stocktakeService;
    private final ExcelStocktakeExportService excelStocktakeExportService;

    @GetMapping("/new")
    public String newStocktakePage(
            Model model,
            Authentication authentication) {

        User currentUser = userService.getCurrentUser(authentication);

        boolean canViewPurchasePrice =
                currentUser.getRole() == UserRole.ADMIN
                        || currentUser.getRole() == UserRole.OWNER;

        model.addAttribute(
                "products",
                productService.getAllProducts().stream()
                        .map(product -> ProductListItemDto.from(
                                product,
                                canViewPurchasePrice
                        ))
                        .toList()
        );

        model.addAttribute("activeSection", "stocktakes");

        return "stocktakes/stocktake-new";
    }

    @GetMapping
    public String stocktakesPage(Model model) {

        model.addAttribute(
                "stocktakes",
                stocktakeService.getAllStocktakes()
        );

        model.addAttribute("activeSection", "stocktakes");

        return "stocktakes/stocktakes-list";
    }

    @GetMapping("/{id}")
    public String stocktakeDetailsPage(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "stocktake",
                stocktakeService.getStocktakeById(id)
        );

        model.addAttribute(
                "stocktakeCountRequest",
                stocktakeService.createCountRequest(id)
        );

        model.addAttribute("activeSection", "stocktakes");

        return "stocktakes/stocktake-details";
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportStocktake(
            @PathVariable Long id) throws IOException {

        byte[] file = excelStocktakeExportService.exportExcel(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stocktake-" + id + ".xlsx\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @GetMapping("/{id}/import/preview")
    public String importPreviewPage(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Object previewObject = session.getAttribute(
                "stocktakeImportPreview_" + id
        );

        if (!(previewObject instanceof StocktakeImportPreviewDto preview)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Няма наличен предварителен преглед. Качи Excel файла отново."
            );

            return "redirect:/stocktakes/" + id;
        }

        model.addAttribute("stocktake", stocktakeService.getStocktakeById(id));
        model.addAttribute("preview", preview);
        model.addAttribute("activeSection", "stocktakes");

        return "stocktakes/stocktake-import-preview";
    }
}
