package com.viecinema.booking.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.service.ComboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Combos", description = "Retrieve food & beverage combo packages available for purchase during booking")
public class ComboController {

    private final ComboService comboService;

    @Operation(
            summary = "Get all active combos",
            description = "Returns a list of all currently active food & beverage combo packages that can be added to a booking."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Combos retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<List<ComboInfo>>> getActiveCombos() {
        List<ComboInfo> combos = comboService.getActiveCombos();

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(RESOURCE_RETRIEVED, combos, "Combos"));
    }
}


