package com.opticsappbg.opticsapp.order.repository;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderStatus;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;


public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusNotInOrderByCreatedOnDesc(
            List<OrderStatus> statuses
    );

    Page<Order> findAllByOrderByCreatedOnDesc(Pageable pageable);

    Page<Order> findByCreatedOnBetweenOrderByCreatedOnDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<Order> findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
            LocalDateTime startDate,
            Pageable pageable
    );

    Page<Order> findByCreatedOnLessThanOrderByCreatedOnDesc(
            LocalDateTime endDate,
            Pageable pageable
    );

    List<Order> findByCreatedOnBetweenOrderByCreatedOnDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Order> findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
            LocalDateTime startDate
    );

    List<Order> findByCreatedOnLessThanOrderByCreatedOnDesc(
            LocalDateTime endDate
    );

    List<Order> findAllByOrderByCreatedOnDesc();

    @Query(
            value = """
            SELECT o FROM Order o
            JOIN o.customer c
            WHERE (
                (
                    :search IS NULL
                    AND :searchDateStart IS NULL
                    AND :searchDay IS NULL
                )
                OR (
                    :search IS NOT NULL
                    AND (
                        LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR c.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (
                    :searchDateStart IS NOT NULL
                    AND o.createdOn >= :searchDateStart
                    AND o.createdOn < :searchDateEnd
                )
                OR (
                    :searchDay IS NOT NULL
                    AND CAST(strftime('%d', o.createdOn) AS INTEGER) = :searchDay
                    AND CAST(strftime('%m', o.createdOn) AS INTEGER) = :searchMonth
                )
            )
            AND (:startDate IS NULL OR o.createdOn >= :startDate)
            AND (:endDate IS NULL OR o.createdOn < :endDate)
            ORDER BY o.createdOn DESC
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            JOIN o.customer c
            WHERE (
                (
                    :search IS NULL
                    AND :searchDateStart IS NULL
                    AND :searchDay IS NULL
                )
                OR (
                    :search IS NOT NULL
                    AND (
                        LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR c.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (
                    :searchDateStart IS NOT NULL
                    AND o.createdOn >= :searchDateStart
                    AND o.createdOn < :searchDateEnd
                )
                OR (
                    :searchDay IS NOT NULL
                    AND CAST(strftime('%d', o.createdOn) AS INTEGER) = :searchDay
                    AND CAST(strftime('%m', o.createdOn) AS INTEGER) = :searchMonth
                )
            )
            AND (:startDate IS NULL OR o.createdOn >= :startDate)
            AND (:endDate IS NULL OR o.createdOn < :endDate)
            """
    )
    Page<Order> searchOrders(
            @Param("search") String search,
            @Param("searchDateStart") LocalDateTime searchDateStart,
            @Param("searchDateEnd") LocalDateTime searchDateEnd,
            @Param("searchDay") Integer searchDay,
            @Param("searchMonth") Integer searchMonth,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items items
            LEFT JOIN FETCH items.product
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.createdBy
            WHERE o.status <> :cancelledStatus
              AND (:fromDate IS NULL OR o.createdOn >= :fromDate)
              AND (:toDate IS NULL OR o.createdOn < :toDate)
              AND (:customerId IS NULL OR o.customer.id = :customerId)
              AND (:userId IS NULL OR o.createdBy.id = :userId)
              AND (
                    :filterStatuses = false
                    OR o.status IN :statuses
              )
              AND (
                    :filterProducts = false
                    OR EXISTS (
                        SELECT 1
                        FROM OrderItem filterItem
                        JOIN filterItem.product filterProduct
                        WHERE filterItem.order = o
                          AND (:filterProductIds = false
                               OR filterProduct.id IN :productIds)
                          AND (:filterCategories = false
                               OR filterProduct.category IN :categories)
                          AND (:brand IS NULL
                               OR LOWER(filterProduct.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
                          AND (:model IS NULL
                               OR LOWER(filterProduct.model) LIKE LOWER(CONCAT('%', :model, '%')))
                          AND (:barcode IS NULL
                               OR LOWER(filterProduct.barcode) LIKE LOWER(CONCAT('%', :barcode, '%')))
                    )
              )
            ORDER BY o.createdOn DESC
            """)
    List<Order> findSalesForReport(
            @Param("cancelledStatus") OrderStatus cancelledStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("customerId") Long customerId,
            @Param("userId") Long userId,
            @Param("filterStatuses") boolean filterStatuses,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("filterProducts") boolean filterProducts,
            @Param("filterProductIds") boolean filterProductIds,
            @Param("productIds") List<Long> productIds,
            @Param("filterCategories") boolean filterCategories,
            @Param("categories") List<ProductCategory> categories,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("barcode") String barcode
    );
}
