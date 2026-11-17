package com.opticsappbg.opticsapp.product.service;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelProductExportService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public byte[] exportExcel(
            List<Long> productIds,
            boolean includePurchasePrice) throws IOException {

        List<Long> ids = productIds == null
                ? List.of()
                : productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Product> productsById =
                productRepository.findAllById(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                product -> product
                        ));

        // Запазваме реда на продуктите от екрана.
        List<Product> products = ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList();

        List<String> headers = new ArrayList<>(List.of(
                "Категория",
                "Бранд",
                "Модел",
                "Баркод",
                "Наличност",
                "Размер",
                "SPH",
                "CYL",
                "ADD",
                "Призма"
        ));

        if (includePurchasePrice) {
            headers.add("Доставна цена");
        }

        headers.addAll(List.of(
                "Продажна цена",
                "Бележка"
        ));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Продукти");

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

            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);

                int column = 0;

                setText(row, column++,
                        product.getCategory() != null
                                ? product.getCategory().getDisplayName()
                                : null);

                setText(row, column++, product.getBrand());
                setText(row, column++, product.getModel());
                setText(row, column++, product.getBarcode());

                Cell quantityCell = row.createCell(column++);
                if (product.getQuantity() != null) {
                    quantityCell.setCellValue(product.getQuantity());
                }

                setText(row, column++, product.getSizeOrDiameter());
                setNumber(row, column++, product.getSph());
                setCharacteristic(row, column++, product.getCyl());
                setCharacteristic(row, column++, product.getAddPower());
                setCharacteristic(row, column++, product.getPrism());

                if (includePurchasePrice) {
                    setNumber(row, column++, product.getPurchasePrice());
                }

                setNumber(row, column++, product.getRetailPrice());
                setText(row, column, product.getNote());
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

    private void setNumber(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private void setCharacteristic(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);

        if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
            cell.setCellValue(value.doubleValue());

            CellStyle style = row.getSheet().getWorkbook().createCellStyle();
            style.setDataFormat(
                    row.getSheet().getWorkbook()
                            .createDataFormat()
                            .getFormat("0.00")
            );

            cell.setCellStyle(style);
        }
    }
}
