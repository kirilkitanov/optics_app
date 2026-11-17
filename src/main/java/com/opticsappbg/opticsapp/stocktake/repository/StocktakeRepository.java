package com.opticsappbg.opticsapp.stocktake.repository;

import com.opticsappbg.opticsapp.stocktake.model.Stocktake;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {

    List<Stocktake> findAllByOrderByCreatedOnDesc();

    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "createdBy"
    })
    Optional<Stocktake> findWithItemsAndProductsById(Long id);
}
