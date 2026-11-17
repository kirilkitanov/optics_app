package com.opticsappbg.opticsapp.web.dto;

import java.util.List;

public record StocktakeImportPreviewDto(

        Long stocktakeId,

        List<StocktakeImportRowDto> rows,

        int validRows,

        int errorRows,

        int ignoredRows

) {

    public boolean hasErrors() {
        return errorRows > 0;
    }
}
