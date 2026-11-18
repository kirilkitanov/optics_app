package com.opticsappbg.opticsapp.report.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportSortDirection {

    ASC("Възходящо"),
    DESC("Низходящо");

    private final String displayName;
}