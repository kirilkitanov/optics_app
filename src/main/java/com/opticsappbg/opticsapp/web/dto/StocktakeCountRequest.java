package com.opticsappbg.opticsapp.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StocktakeCountRequest {

    private List<StocktakeCountItemRequest> items;

    @Getter
    @Setter
    public static class StocktakeCountItemRequest {

        private Long itemId;

        private Integer countedQuantity;
    }
}