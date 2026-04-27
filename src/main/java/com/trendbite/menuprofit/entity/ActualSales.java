package com.trendbite.menuprofit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "actual_sales",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_actual_sales_menu_date",
                        columnNames = {"menu_id", "sales_date"})
        })
@Getter @Setter
@NoArgsConstructor
public class ActualSales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private LocalDate salesDate;       // 판매 날짜
    private Integer actualQuantity;    // 실제 판매량 (개)
    private Integer actualRevenue;     // 실제 매출 (원)
}