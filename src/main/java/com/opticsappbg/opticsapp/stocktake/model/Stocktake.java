package com.opticsappbg.opticsapp.stocktake.model;

import com.opticsappbg.opticsapp.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stocktakes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stocktake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    private LocalDateTime importedOn;

    @Transient
    private Integer totalDifference;

    @Builder.Default
    @OneToMany(mappedBy = "stocktake",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<StocktakeItem> items = new ArrayList<>();
}
