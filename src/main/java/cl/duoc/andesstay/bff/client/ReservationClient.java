package cl.duoc.andesstay.bff.client;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import cl.duoc.andesstay.bff.dto.PageResponse;
import cl.duoc.andesstay.bff.dto.ReservationRequestDto;
import cl.duoc.andesstay.bff.dto.ReservationResponseDto;

/**
 * Encapsula las llamadas HTTP hacia ms-andesstay-reservations.
 *
 * El access token de Cognito del usuario se reenvia en cada llamada gracias
 * a {@link cl.duoc.andesstay.bff.security.BearerTokenPropagationFilter}
 * configurado en el WebClient.
 *
 * Se usa {@code .block(TIMEOUT)} porque el BFF es una app servlet (MVC)
 * clasica: cada request ya corre en su propio hilo, asi que no se busca
 * no-bloqueo aqui, solo reusar WebClient como cliente HTTP moderno.
 */
@Component
public class ReservationClient {

    /** Timeout maximo de espera para cada llamada al microservicio. */
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final WebClient reservationsWebClient;

    public ReservationClient(@Qualifier("reservationsWebClient") WebClient reservationsWebClient) {
        this.reservationsWebClient = reservationsWebClient;
    }

    public ReservationResponseDto crear(ReservationRequestDto request) {
        return reservationsWebClient.post()
                .uri("/api/reservations")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public PageResponse<ReservationResponseDto> listar(String guestId, Long unitId, String estado,
                                                         LocalDate desde, LocalDate hasta,
                                                         int page, int size) {
        return reservationsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/reservations")
                        .queryParamIfPresent("guestId", Optional.ofNullable(guestId))
                        .queryParamIfPresent("unitId", Optional.ofNullable(unitId))
                        .queryParamIfPresent("estado", Optional.ofNullable(estado))
                        .queryParamIfPresent("desde", Optional.ofNullable(desde).map(LocalDate::toString))
                        .queryParamIfPresent("hasta", Optional.ofNullable(hasta).map(LocalDate::toString))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<ReservationResponseDto>>() { })
                .block(TIMEOUT);
    }

    public List<ReservationResponseDto> misReservas() {
        return reservationsWebClient.get()
                .uri("/api/reservations/mias")
                .retrieve()
                .bodyToFlux(ReservationResponseDto.class)
                .collectList()
                .block(TIMEOUT);
    }

    public ReservationResponseDto obtener(Long id) {
        return reservationsWebClient.get()
                .uri("/api/reservations/{id}", id)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public ReservationResponseDto confirmar(Long id) {
        return reservationsWebClient.put()
                .uri("/api/reservations/{id}/confirmar", id)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public ReservationResponseDto checkIn(Long id) {
        return reservationsWebClient.put()
                .uri("/api/reservations/{id}/checkin", id)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public ReservationResponseDto checkOut(Long id) {
        return reservationsWebClient.put()
                .uri("/api/reservations/{id}/checkout", id)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public ReservationResponseDto cancelar(Long id) {
        return reservationsWebClient.put()
                .uri("/api/reservations/{id}/cancelar", id)
                .retrieve()
                .bodyToMono(ReservationResponseDto.class)
                .block(TIMEOUT);
    }

    public void eliminar(Long id) {
        reservationsWebClient.delete()
                .uri("/api/reservations/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }
}
