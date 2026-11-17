
package com.opticsappbg.opticsapp.stocktake.repository;

import com.opticsappbg.opticsapp.stocktake.model.StocktakeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {

    List<StocktakeItem> findAllByStocktake_Id(Long stocktakeId);

    Optional<StocktakeItem> findByStocktake_IdAndProduct_Id(
            Long stocktakeId,
            Long productId
    );
}
