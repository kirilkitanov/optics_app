package com.opticsappbg.opticsapp.web.dto;

public record StocktakeImportRowDto(

        int rowNumber,

        Long productId,

        String productName,

        Integer initialQuantity,

        String importedQuantity,

        Integer countedQuantity,

        Integer difference,

        String error
) {
}
