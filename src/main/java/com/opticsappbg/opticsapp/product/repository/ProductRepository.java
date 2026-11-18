package com.opticsappbg.opticsapp.product.repository;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.category = :category
              AND p.brand = :brand
              AND p.model = :model
              AND (p.barcode = :barcode OR (p.barcode IS NULL AND :barcode IS NULL))
              AND (p.sizeOrDiameter = :sizeOrDiameter OR (p.sizeOrDiameter IS NULL AND :sizeOrDiameter IS NULL))
              AND p.sph = :sph
              AND p.cyl = :cyl
              AND p.addPower = :addPower
              AND p.prism = :prism
            """)
    Optional<Product> findExactProduct(
            @Param("category") ProductCategory category,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("barcode") String barcode,
            @Param("sizeOrDiameter") String sizeOrDiameter,
            @Param("sph") BigDecimal sph,
            @Param("cyl") BigDecimal cyl,
            @Param("addPower") BigDecimal addPower,
            @Param("prism") BigDecimal prism
    );

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.category = :category
              AND p.brand = :brand
              AND p.model = :model
              AND (p.sizeOrDiameter = :sizeOrDiameter OR (p.sizeOrDiameter IS NULL AND :sizeOrDiameter IS NULL))
              AND p.sph = :sph
              AND p.cyl = :cyl
              AND p.addPower = :addPower
              AND p.prism = :prism
            """)
    List<Product> findProductsByCharacteristics(
            @Param("category") ProductCategory category,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("sizeOrDiameter") String sizeOrDiameter,
            @Param("sph") BigDecimal sph,
            @Param("cyl") BigDecimal cyl,
            @Param("addPower") BigDecimal addPower,
            @Param("prism") BigDecimal prism
    );

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND TRIM(p.brand) != ''")
    List<String> findDistinctBrands();

    List<Product> findAllByBarcode(String barcode);

    @Query("""
    SELECT p FROM Product p
    WHERE p.category = :category
    AND p.brand = :brand
    AND p.model = :model
    AND p.sph = :sph
    AND p.cyl = :cyl
    AND p.addPower = :addPower
    AND p.prism = :prism
    """)
    List<Product> findProductsByCharacteristicsWithoutSize(
            @Param("category") ProductCategory category,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("sph") BigDecimal sph,
            @Param("cyl") BigDecimal cyl,
            @Param("addPower") BigDecimal addPower,
            @Param("prism") BigDecimal prism
    );
    @Query("SELECT DISTINCT p.category, p.brand, p.model FROM Product p ORDER BY p.category, p.brand, p.model")
    List<Object[]> findDistinctReportProductFilters();

    @Query(value = """
            SELECT p.* FROM products p
            WHERE (:categoryCsv = '' OR instr(',' || :categoryCsv || ',', ',' || p.category || ',') > 0)
              AND (:brand = '' OR p.brand = :brand)
              AND (:model = '' OR p.model = :model)
              AND lower(coalesce(p.brand, '') || ' ' || coalesce(p.model, '') || ' ' ||
                    's' || printf('%.2f', p.sph) || ' ' ||
                    'c' || printf('%.2f', p.cyl) || ' ' ||
                    'a' || printf('%.2f', p.add_power) || ' ' ||
                    'p' || printf('%.2f', p.prism) || ' ' ||
                    coalesce(p.size_or_diameter, '') || ' ' || coalesce(p.barcode, ''))
                    LIKE '%' || lower(:query) || '%'
            ORDER BY p.brand COLLATE NOCASE, p.model COLLATE NOCASE, p.sph, p.cyl
            LIMIT 40
            """, nativeQuery = true)
    List<Product> searchReportProducts(
            @Param("query") String query,
            @Param("categoryCsv") String categoryCsv,
            @Param("brand") String brand,
            @Param("model") String model
    );
}

