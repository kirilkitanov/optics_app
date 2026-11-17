package com.opticsappbg.opticsapp.stocktake.service;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.service.ActivityLogService;
import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import com.opticsappbg.opticsapp.stocktake.model.Stocktake;
import com.opticsappbg.opticsapp.stocktake.model.StocktakeItem;
import com.opticsappbg.opticsapp.stocktake.repository.StocktakeRepository;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final ProductRepository productRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public Stocktake createStocktake(List<Long> productIds, User createdBy) {

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Не са избрани продукти за ревизията."
            );
        }

        if (createdBy == null) {
            throw new IllegalArgumentException(
                    "Не е намерен потребителят, създаващ ревизията."
            );
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(productIds);

        if (uniqueIds.contains(null)) {
            throw new IllegalArgumentException(
                    "Списъкът съдържа невалиден продукт."
            );
        }

        List<Product> products =
                productRepository.findAllById(uniqueIds);

        if (products.size() != uniqueIds.size()) {
            throw new IllegalArgumentException(
                    "Някои от избраните продукти не съществуват."
            );
        }

        Stocktake stocktake = Stocktake.builder()
                .createdOn(LocalDateTime.now())
                .createdBy(createdBy)
                .build();

        for (Product product : products) {

            StocktakeItem item = StocktakeItem.builder()
                    .stocktake(stocktake)
                    .product(product)
                    .initialQuantity(
                            product.getQuantity() != null
                                    ? product.getQuantity()
                                    : 0
                    )
                    .countedQuantity(null)
                    .appliedDifference(0)
                    .build();

            stocktake.getItems().add(item);
        }

        Stocktake savedStocktake = stocktakeRepository.save(stocktake);

        activityLogService.log(
                ActivityAction.CREATE,
                "STOCKTAKE",
                savedStocktake.getId(),
                "Създадена складова ревизия №" + savedStocktake.getId()
        );

        return savedStocktake;
    }

    @Transactional
    public void saveCountedQuantities(
            Long stocktakeId,
            StocktakeCountRequest request) {

        if (request == null || request.getItems() == null) {
            throw new IllegalArgumentException(
                    "Липсват данни за преброените количества."
            );
        }

        Stocktake stocktake = stocktakeRepository
                .findWithItemsAndProductsById(stocktakeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не е намерена складова ревизия с ID: " + stocktakeId
                ));

        Set<Long> processedItemIds = new HashSet<>();
        int updatedItems = 0;

        for (StocktakeCountRequest.StocktakeCountItemRequest entry
                : request.getItems()) {

            if (entry == null || entry.getItemId() == null) {
                throw new IllegalArgumentException(
                        "Получени са невалидни данни за продукт."
                );
            }

            Long itemId = entry.getItemId();
            Integer countedQuantity = entry.getCountedQuantity();

            if (!processedItemIds.add(itemId)) {
                throw new IllegalArgumentException(
                        "Продуктът присъства повече от веднъж в заявката."
                );
            }

            StocktakeItem item = stocktake.getItems().stream()
                    .filter(stocktakeItem ->
                            stocktakeItem.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Продуктът не принадлежи към тази ревизия."
                    ));

            // Празното поле означава, че не променяме продукта.
            if (countedQuantity == null) {
                continue;
            }

            Product product = item.getProduct();

            int currentQuantity = product.getQuantity() != null
                    ? product.getQuantity()
                    : 0;

            int oldAppliedDifference = item.getAppliedDifference() != null
                    ? item.getAppliedDifference()
                    : 0;

            int newDifference =
                    countedQuantity - item.getInitialQuantity();

            int newCurrentQuantity =
                    currentQuantity - oldAppliedDifference + newDifference;

            // Коригираме текущата складова наличност.
            product.setQuantity(newCurrentQuantity);

            // Запазваме новото преброяване и приложената разлика.
            item.setCountedQuantity(countedQuantity);
            item.setAppliedDifference(newDifference);

            updatedItems++;
        }

        if (updatedItems > 0) {

            stocktakeRepository.save(stocktake);

            activityLogService.log(
                    ActivityAction.UPDATE,
                    "STOCKTAKE",
                    stocktake.getId(),
                    "Обновена складова ревизия №" + stocktake.getId()
                            + ". Променени продукти: " + updatedItems
            );
        }
    }

    @Transactional(readOnly = true)
    public com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest
    createCountRequest(Long stocktakeId) {

        Stocktake stocktake = getStocktakeById(stocktakeId);

        com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest request =
                new com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest();

        List<com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest.StocktakeCountItemRequest>
                items = new java.util.ArrayList<>();

        for (StocktakeItem stocktakeItem : stocktake.getItems()) {

            var itemRequest =
                    new com.opticsappbg.opticsapp.web.dto.StocktakeCountRequest.StocktakeCountItemRequest();

            itemRequest.setItemId(stocktakeItem.getId());
            itemRequest.setCountedQuantity(stocktakeItem.getCountedQuantity());

            items.add(itemRequest);
        }

        request.setItems(items);

        return request;
    }

    @Transactional(readOnly = true)
    public List<Stocktake> getAllStocktakes() {

        List<Stocktake> stocktakes =
                stocktakeRepository.findAllByOrderByCreatedOnDesc();

        stocktakes.forEach(this::calculateTotalDifference);

        return stocktakes;
    }

    private void calculateTotalDifference(Stocktake stocktake) {

        int totalDifference = 0;

        for (StocktakeItem item : stocktake.getItems()) {

            if (item.getCountedQuantity() != null) {
                totalDifference +=
                        item.getCountedQuantity() - item.getInitialQuantity();
            }
        }

        stocktake.setTotalDifference(totalDifference);
    }

    @Transactional(readOnly = true)
    public Stocktake getStocktakeById(Long id) {

        Stocktake stocktake = stocktakeRepository
                .findWithItemsAndProductsById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не е намерена складова ревизия с ID: " + id
                ));

        calculateTotalDifference(stocktake);

        return stocktake;
    }
}
