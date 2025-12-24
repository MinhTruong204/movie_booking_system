package com.viecinema.booking.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.booking.dto.ComboDto;
import com.viecinema.booking.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.COMBO_PATH;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@RestController
@RequestMapping(COMBO_PATH)
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComboDto>>> getActiveCombos() {
        List<ComboDto> combos = comboService.getActiveCombos();

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED,combos,"Combos"));
    }
}
