package com.viecinema.booking.service;

import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ComboService {

    private final ComboRepository comboRepository;

    public List<ComboInfo> getActiveCombos() {
        log.info("Fetching active combos");

        List<Combo> combos = comboRepository.findByIsActiveTrue();

        return combos.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<Combo> getCombosByIds(List<Integer> ids) {
        return comboRepository.findByIdInAndIsActiveTrue(ids);
    }

    private ComboInfo convertToDto(Combo combo) {
        return ComboInfo.builder()
                .comboId(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .price(combo.getPrice())
                .imageUrl(combo.getImageUrl())
                .isActive(combo.getIsActive())
                .build();
    }
}
