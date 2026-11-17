package com.opticsappbg.opticsapp.web;

import com.opticsappbg.opticsapp.stocktake.model.Stocktake;
import com.opticsappbg.opticsapp.stocktake.service.ExcelStocktakeImportService;
import com.opticsappbg.opticsapp.stocktake.service.StocktakeService;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.service.UserService;
import com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest;
import com.opticsappbg.opticsapp.web.dto.StocktakeImportPreviewDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;
    private final UserService userService;
    private final ExcelStocktakeImportService excelStocktakeImportService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(10000);
    }

    @PostMapping
    public String createStocktake(
            @RequestParam("productIds") List<Long> productIds,
            Authentication authentication) {

        User currentUser = userService.getCurrentUser(authentication);

        Stocktake stocktake = stocktakeService.createStocktake(
                productIds,
                currentUser
        );

        return "redirect:/stocktakes/" + stocktake.getId();
    }

    @PostMapping("/{id}/save")
    public String saveStocktake(
            @PathVariable Long id,
            @ModelAttribute StocktakeCountRequest request,
            RedirectAttributes redirectAttributes) {

        stocktakeService.saveCountedQuantities(id, request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Ревизията е запазена успешно."
        );

        return "redirect:/stocktakes";
    }

    @PostMapping("/{id}/import/preview")
    public String previewImport(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            StocktakeImportPreviewDto preview =
                    excelStocktakeImportService.previewExcel(id, file);

            session.setAttribute(
                    "stocktakeImportPreview_" + id,
                    preview
            );

            return "redirect:/stocktakes/" + id + "/import/preview";

        } catch (IOException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/stocktakes/" + id;
        }
    }

    @PostMapping("/{id}/import/confirm")
    public String confirmImport(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String sessionKey = "stocktakeImportPreview_" + id;

        Object previewObject = session.getAttribute(sessionKey);

        if (!(previewObject instanceof StocktakeImportPreviewDto preview)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Няма наличен предварителен преглед. Качи Excel файла отново."
            );

            return "redirect:/stocktakes/" + id;
        }

        try {
            if (!id.equals(preview.stocktakeId())) {
                throw new IllegalArgumentException(
                        "Данните за импорта не съответстват на ревизията."
                );
            }

            excelStocktakeImportService.applyImport(preview);

            session.removeAttribute(sessionKey);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Импортът е завършен успешно."
            );

            return "redirect:/stocktakes/" + id;

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/stocktakes/" + id + "/import/preview";
        }
    }
}
