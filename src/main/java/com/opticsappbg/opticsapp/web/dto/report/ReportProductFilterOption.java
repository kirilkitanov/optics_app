package com.opticsappbg.opticsapp.web.dto.report;

import com.opticsappbg.opticsapp.product.model.ProductCategory;

public record ReportProductFilterOption(ProductCategory category, String brand, String model) { }
