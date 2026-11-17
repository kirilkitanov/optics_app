package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.report.model.ReportMetric;
import com.opticsappbg.opticsapp.report.model.ReportSource;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReportSection {

    private ReportSource source;
    private String title;
    private List<ReportMetric> columns = new ArrayList<>();
    private List<ReportRow> rows = new ArrayList<>();
    private ReportTotals totals = new ReportTotals();
}
