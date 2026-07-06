package com.viecinema.loyalty.mapper;

import com.viecinema.loyalty.dto.LoyaltyHistoryItemDto;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface LoyaltyMapper {
    @Mapping(source = "id", target = "historyId")
    LoyaltyHistoryItemDto toHistoryItemDto(LoyaltyPointsHistory history);
}
