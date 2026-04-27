package com.trendbite.menuprofit.trend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trend_keyword",
        uniqueConstraints = @UniqueConstraint(name = "uk_trend_keyword", columnNames = {"keyword", "category"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Long searchCount;

    @Column(nullable = false)
    private LocalDateTime firstSearchedAt;

    @Column(nullable = false)
    private LocalDateTime lastSearchedAt;

    public void incrementSearchCount() {
        this.searchCount = (this.searchCount == null ? 0L : this.searchCount) + 1L;
        this.lastSearchedAt = LocalDateTime.now();
    }
}
