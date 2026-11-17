package com.opticsappbg.opticsapp.web.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReportResult {

    private ReportFilter filter;
    private LocalDateTime generatedOn;
    private List<ReportSection> sections = new ArrayList<>();
}
