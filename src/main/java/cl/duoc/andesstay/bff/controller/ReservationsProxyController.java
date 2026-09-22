package cl.duoc.andesstay.bff.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.andesstay.bff.client.ReservationClient;
import cl.duoc.andesstay.bff.dto.PageResponse;
import cl.duoc.andesstay.bff.dto.ReservationRequestDto;
import cl.duoc.andesstay.bff.dto.ReservationResponseDto;
import jakarta.validation.Valid;

/**
 * Puerta de entrada del frontend Angular hacia el dominio de reservas.
 * Igual que CatalogProxyController: sin logica de negocio, solo valida y
 * reenvia hacia ms-andesstay-reservations via ReservationClient.
 *
 * SecurityConfig solo exige "autenticado" para estas rutas; el detalle por
 * rol y por dueno de la reserva lo resuelve reservations con @PreAuthorize
 * (ver comentario en ReservationClient).
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationsProxyController {

    private final ReservationClient reservationClient;

    public ReservationsProxyController(ReservationClient reservationClient) {
        this.reservationClient = reservationClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponseDto crear(@Valid @RequestBody ReservationRequestDto request) {
        return reservationClient.crear(request);
    }

    @GetMapping
    public PageResponse<ReservationResponseDto> listar(
            @RequestParam(required = false) String guestId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reservationClient.listar(guestId, unitId, estado, desde, hasta, page, size);
    }

    @GetMapping("/mias")
    public List<ReservationResponseDto> misReservas() {
        return reservationClient.misReservas();
    }

    @GetMapping("/{id}")
    public ReservationResponseDto obtener(@PathVariable Long id) {
        return reservationClient.obtener(id);
    }

    @PutMapping("/{id}/confirmar")
    public ReservationResponseDto confirmar(@PathVariable Long id) {
        return reservationClient.confirmar(id);
    }

    @PutMapping("/{id}/checkin")
    public ReservationResponseDto checkIn(@PathVariable Long id) {
        return reservationClient.checkIn(id);
    }

    @PutMapping("/{id}/checkout")
    public ReservationResponseDto checkOut(@PathVariable Long id) {
        return reservationClient.checkOut(id);
    }

    @PutMapping("/{id}/cancelar")
    public ReservationResponseDto cancelar(@PathVariable Long id) {
        return reservationClient.cancelar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reservationClient.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}