package com.trendbite.menuprofit.trend.dto.response;

import com.trendbite.menuprofit.trend.entity.TrendKeyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendKeywordResponse {

    private String keyword;
    private String category;
    private Long searchCount;
    private LocalDateTime lastSearchedAt;

    public static TrendKeywordResponse from(TrendKeyword entity) {
        return TrendKeywordResponse.builder()
                .keyword(entity.getKeyword())
                .category(entity.getCategory())
                .searchCount(entity.getSearchCount())
                .lastSearchedAt(entity.getLastSearchedAt())
                .build();
    }
}
