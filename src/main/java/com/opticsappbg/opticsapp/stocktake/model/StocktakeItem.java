package com.opticsappbg.opticsapp.stocktake.model;

import com.opticsappbg.opticsapp.product.model.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocktake_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"stocktake_id", "product_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stocktake_id", nullable = false)
    private Stocktake stocktake;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer initialQuantity;

    private Integer countedQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer appliedDifference = 0;
}
