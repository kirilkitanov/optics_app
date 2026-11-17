package com.opticsappbg.opticsapp.stocktake.service;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.service.ActivityLogService;
import com.opticsappbg.opticsapp.stocktake.model.StocktakeItem;
import com.opticsappbg.opticsapp.stocktake.model.Stocktake;
import com.opticsappbg.opticsapp.stocktake.repository.StocktakeItemRepository;
import com.opticsappbg.opticsapp.stocktake.repository.StocktakeRepository;
import com.opticsappbg.opticsapp.web.dto.StocktakeImportPreviewDto;
import com.opticsappbg.opticsapp.web.dto.StocktakeImportRowDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelStocktakeImportService {

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository stocktakeItemRepository;
    private final ActivityLogService activityLogService;

    private static final List<String> EXPECTED_HEADERS = List.of(
            "ID на продукт",
            "Категория",
            "Продукт",
            "Баркод",
            "Размер / Диаметър",
            "SPH / CYL / ADD / Призма",
            "Начална наличност",
            "Преброено количество"
    );

    @Transactional(readOnly = true)
    public StocktakeImportPreviewDto previewExcel(
            Long stocktakeId,
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Моля, изберете Excel файл.");
        }

        if (!stocktakeRepository.existsById(stocktakeId)) {
            throw new IllegalArgumentException(
                    "Не е намерена складова ревизия с ID: " + stocktakeId
            );
        }

        Map<Long, StocktakeItem> itemsByProductId = new HashMap<>();

        for (StocktakeItem item :
                stocktakeItemRepository.findAllByStocktake_Id(stocktakeId)) {

            itemsByProductId.put(item.getProduct().getId(), item);
        }

        List<StocktakeImportRowDto> previewRows = new ArrayList<>();

        int validRows = 0;
        int errorRows = 0;
        int ignoredRows = 0;

        Set<Long> processedProductIds = new HashSet<>();

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel файлът няма работен лист."
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Липсва заглавният ред в Excel файла."
                );
            }

            validateHeaders(headerRow, formatter);

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;

                String importedQuantity =
                        getCellText(row.getCell(7), formatter);

                if (importedQuantity.isBlank()) {

                    Long productId = parseProductId(row.getCell(0));

                    StocktakeItem item = productId != null
                            ? itemsByProductId.get(productId)
                            : null;

                    previewRows.add(new StocktakeImportRowDto(
                            excelRowNumber,
                            productId,
                            item != null ? getProductName(item) : null,
                            item != null ? item.getInitialQuantity() : null,
                            "",
                            null,
                            null,
                            null
                    ));

                    ignoredRows++;
                    continue;
                }

                String error = null;
                Long productId = null;
                StocktakeItem item = null;
                Integer countedQuantity = null;
                Integer initialQuantity = null;
                Integer difference = null;

                try {
                    productId = parseProductId(row.getCell(0));

                    if (productId == null) {
                        throw new IllegalArgumentException(
                                "Липсва валидно ID на продукт."
                        );
                    }

                    if (!processedProductIds.add(productId)) {
                        throw new IllegalArgumentException(
                                "Продуктът е посочен повече от веднъж."
                        );
                    }

                    item = itemsByProductId.get(productId);

                    if (item == null) {
                        throw new IllegalArgumentException(
                                "Продуктът не принадлежи към тази ревизия."
                        );
                    }

                    initialQuantity = item.getInitialQuantity();

                    Integer excelInitialQuantity =
                            parseInteger(row.getCell(6));

                    if (excelInitialQuantity == null) {
                        throw new IllegalArgumentException(
                                "Липсва валидна начална наличност."
                        );
                    }

                    if (!initialQuantity.equals(excelInitialQuantity)) {
                        throw new IllegalArgumentException(
                                "Началната наличност в Excel не съвпада с ревизията."
                        );
                    }

                    countedQuantity = parseInteger(
                            row.getCell(7)
                    );

                    if (countedQuantity == null) {
                        throw new IllegalArgumentException(
                                "Невалидно преброено количество."
                        );
                    }

                    difference = countedQuantity - initialQuantity;

                } catch (IllegalArgumentException ex) {
                    error = ex.getMessage();
                }

                previewRows.add(new StocktakeImportRowDto(
                        excelRowNumber,
                        productId,
                        item != null ? getProductName(item) : null,
                        initialQuantity,
                        importedQuantity,
                        countedQuantity,
                        difference,
                        error
                ));

                if (error == null) {
                    validRows++;
                } else {
                    errorRows++;
                }
            }

        } catch (org.apache.poi.EncryptedDocumentException ex) {
            throw new IllegalArgumentException(
                    "Excel файлът е защитен с парола."
            );
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException ex) {
            throw new IllegalArgumentException(
                    "Файлът не е във формат .xlsx."
            );
        }

        return new StocktakeImportPreviewDto(
                stocktakeId,
                previewRows,
                validRows,
                errorRows,
                ignoredRows
        );
    }

    @Transactional
    public void applyImport(StocktakeImportPreviewDto preview) {

        if (preview == null || preview.stocktakeId() == null) {
            throw new IllegalArgumentException(
                    "Липсват данни за импорта."
            );
        }

        if (preview.hasErrors()) {
            throw new IllegalArgumentException(
                    "Импортът съдържа грешки и не може да бъде потвърден."
            );
        }

        if (preview.validRows() == 0) {
            throw new IllegalArgumentException(
                    "Няма валидни редове за импорт."
            );
        }

        Long stocktakeId = preview.stocktakeId();

        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не е намерена складова ревизия."
                ));

        Map<Long, StocktakeItem> itemsByProductId = new HashMap<>();

        for (StocktakeItem item :
                stocktakeItemRepository.findAllByStocktake_Id(stocktakeId)) {

            itemsByProductId.put(item.getProduct().getId(), item);
        }

        Set<Long> processedProductIds = new HashSet<>();

        int appliedRows = 0;

        for (StocktakeImportRowDto row : preview.rows()) {

            // Празните количества не променят нищо.
            if (row.countedQuantity() == null) {
                continue;
            }

            if (row.error() != null) {
                throw new IllegalArgumentException(
                        "Импортът съдържа невалиден ред."
                );
            }

            Long productId = row.productId();

            if (productId == null
                    || !processedProductIds.add(productId)) {
                throw new IllegalArgumentException(
                        "Невалидно или дублирано ID на продукт."
                );
            }

            StocktakeItem item = itemsByProductId.get(productId);

            if (item == null) {
                throw new IllegalArgumentException(
                        "Продуктът с ID " + productId
                                + " не принадлежи към тази ревизия."
                );
            }

            if (row.initialQuantity() == null
                    || !item.getInitialQuantity().equals(row.initialQuantity())) {
                throw new IllegalArgumentException(
                        "Началната наличност на продукт с ID "
                                + productId + " не съвпада."
                );
            }

            int newDifference;

            int newQuantity;

            try {
                newDifference = Math.subtractExact(
                        row.countedQuantity(),
                        item.getInitialQuantity()
                );

                int currentQuantity = item.getProduct().getQuantity() != null
                        ? item.getProduct().getQuantity()
                        : 0;

                int oldAppliedDifference = item.getAppliedDifference() != null
                        ? item.getAppliedDifference()
                        : 0;

                newQuantity = Math.addExact(
                        Math.subtractExact(
                                currentQuantity,
                                oldAppliedDifference
                        ),
                        newDifference
                );

            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                        "Получава се количество извън допустимия диапазон "
                                + "за продукт с ID " + productId + "."
                );
            }

            item.getProduct().setQuantity(newQuantity);

            item.setCountedQuantity(row.countedQuantity());
            item.setAppliedDifference(newDifference);

            appliedRows++;
        }

        if (appliedRows != preview.validRows()) {
            throw new IllegalArgumentException(
                    "Броят на валидните редове не съвпада с данните за импорт."
            );
        }

        stocktake.setImportedOn(LocalDateTime.now());

        stocktakeRepository.save(stocktake);

        activityLogService.log(
                ActivityAction.IMPORT,
                "STOCKTAKE",
                stocktakeId,
                "Импортирани са " + appliedRows
                        + " продукта в складова ревизия №" + stocktakeId + "."
        );
    }

    private void validateHeaders(
            Row headerRow,
            DataFormatter formatter) {

        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {

            String actual = getCellText(
                    headerRow.getCell(i),
                    formatter
            );

            if (!EXPECTED_HEADERS.get(i).equals(actual)) {
                throw new IllegalArgumentException(
                        "Невалидна Excel структура. "
                                + "Очаквана колона: "
                                + EXPECTED_HEADERS.get(i)
                );
            }
        }
    }


    private Long parseProductId(Cell cell) {

        if (cell == null
                || cell.getCellType() == CellType.FORMULA
                || cell.getCellType() == CellType.BOOLEAN
                || cell.getCellType() == CellType.ERROR
                || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {
            BigDecimal value;

            if (cell.getCellType() == CellType.NUMERIC) {
                value = BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();

                if (text.isEmpty()) {
                    return null;
                }

                value = new BigDecimal(text);
            } else {
                return null;
            }

            if (value.signum() <= 0
                    || value.stripTrailingZeros().scale() > 0
                    || value.compareTo(
                    BigDecimal.valueOf(Long.MAX_VALUE)
            ) > 0) {
                return null;
            }

            return value.longValueExact();

        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private Integer parseInteger(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.FORMULA
                || cell.getCellType() == CellType.BOOLEAN
                || cell.getCellType() == CellType.ERROR
                || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {
            BigDecimal value;

            if (cell.getCellType() == CellType.NUMERIC) {
                value = BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();

                if (text.isEmpty()) {
                    return null;
                }

                value = new BigDecimal(text);
            } else {
                return null;
            }

            if (value.stripTrailingZeros().scale() > 0
                    || value.compareTo(
                    BigDecimal.valueOf(Integer.MAX_VALUE)
            ) > 0
                    || value.compareTo(
                    BigDecimal.valueOf(Integer.MIN_VALUE)
            ) < 0) {
                return null;
            }

            return value.intValueExact();

        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }


    private String getCellText(
            Cell cell,
            DataFormatter formatter) {

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private boolean isEmptyRow(
            Row row,
            DataFormatter formatter) {

        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!getCellText(row.getCell(i), formatter).isBlank()) {
                return false;
            }
        }

        return true;
    }

    private String getProductName(StocktakeItem item) {

        String brand = item.getProduct().getBrand() != null
                ? item.getProduct().getBrand()
                : "";

        String model = item.getProduct().getModel() != null
                ? item.getProduct().getModel()
                : "";

        return (brand + " " + model).trim();
    }

}
