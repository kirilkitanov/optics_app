package com.opticsappbg.opticsapp.web.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductHistoryResult {

    private List<Long> productIds = new ArrayList<>();
    private ReportFilter filter;
    private List<ReportSection> sections = new ArrayList<>();
}
