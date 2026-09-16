package cl.duoc.andesstay.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UnitRequestDto(
        @NotBlank String name,
        @NotBlank String type,
        @Positive Integer capacity,
        @Positive BigDecimal pricePerNight,
        Boolean available
) {
}
