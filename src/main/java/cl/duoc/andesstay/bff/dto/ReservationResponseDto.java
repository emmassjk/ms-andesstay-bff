package cl.duoc.andesstay.bff.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationResponseDto(
        Long id,
        Long unitId,
        String guestId,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guestsCount,
        BigDecimal pricePerNight,
        BigDecimal totalPrice,
        String estado,
        Instant createdAt,
        Instant updatedAt
) {
}