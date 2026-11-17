
package com.opticsappbg.opticsapp.stocktake.service;

import com.opticsappbg.opticsapp.stocktake.model.Stocktake;
import com.opticsappbg.opticsapp.stocktake.model.StocktakeItem;
import com.opticsappbg.opticsapp.stocktake.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelStocktakeExportService {

    private final StocktakeRepository stocktakeRepository;

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long stocktakeId) throws IOException {

        Stocktake stocktake = stocktakeRepository
                .findWithItemsAndProductsById(stocktakeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не е намерена складова ревизия с ID: " + stocktakeId
                ));

        List<String> headers = List.of(
                "ID на продукт",
                "Категория",
                "Продукт",
                "Баркод",
                "Размер / Диаметър",
                "SPH / CYL / ADD / Призма",
                "Начална наличност",
                "Преброено количество"
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Складова ревизия");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (StocktakeItem item : stocktake.getItems()) {

                var product = item.getProduct();

                Row row = sheet.createRow(rowIndex++);

                int column = 0;

                row.createCell(column++)
                        .setCellValue(product.getId());

                setText(
                        row,
                        column++,
                        product.getCategory() != null
                                ? product.getCategory().getDisplayName()
                                : null
                );

                String productName =
                        ((product.getBrand() != null ? product.getBrand() : "")
                                + " "
                                + (product.getModel() != null ? product.getModel() : ""))
                                .trim();

                setText(row, column++, productName);
                setText(row, column++, product.getBarcode());
                setText(row, column++, product.getSizeOrDiameter());

                String characteristics = formatCharacteristic("sph", product.getSph(), true)
                        + formatCharacteristic("cyl", product.getCyl(), false)
                        + formatCharacteristic("add", product.getAddPower(), false)
                        + formatCharacteristic("prism", product.getPrism(), false);

                setText(row, column++, characteristics.trim());

                row.createCell(column++)
                        .setCellValue(item.getInitialQuantity());

                // Оставяме клетката празна за ръчно попълване.
                row.createCell(column);
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);

                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setText(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private String formatCharacteristic(
            String label,
            BigDecimal value,
            boolean alwaysShow) {

        if (value == null) {
            return "";
        }

        if (!alwaysShow && value.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }

        return label + ": "
                + value.stripTrailingZeros().toPlainString()
                + "  ";
    }
}
