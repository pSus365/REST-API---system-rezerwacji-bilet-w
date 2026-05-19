package com.example.demo.service;

import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.dto.ReservationResponseDTO;
import com.example.demo.exception.NotEnoughTicketsException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Event;
import com.example.demo.model.Reservation;
import com.example.demo.model.ReservationStatus;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import com.example.demo.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO request) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        AppUser user = appUserRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (event.getAvailableTickets() < request.requestedTickets()) {
            throw new NotEnoughTicketsException("Not enough tickets available for this event");
        }

        event.setAvailableTickets(event.getAvailableTickets() - request.requestedTickets());
        eventRepository.save(event);

        Reservation reservation = Reservation.builder()
                .event(event)
                .user(user)
                .reservationDate(LocalDateTime.now())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, savedReservation.getId());

        return ReservationResponseDTO.builder()
                .id(savedReservation.getId())
                .eventName(event.getName())
                .username(user.getUsername())
                .status(savedReservation.getStatus())
                .build();
    }

    public ReservationResponseDTO getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .eventName(reservation.getEvent().getName())
                .username(reservation.getUser().getUsername())
                .status(reservation.getStatus())
                .build();
    }

    public java.util.List<ReservationResponseDTO> getAllReservations() {
        return reservationRepository.findAll().stream().map(reservation ->
                ReservationResponseDTO.builder()
                        .id(reservation.getId())
                        .eventName(reservation.getEvent().getName())
                        .username(reservation.getUser().getUsername())
                        .status(reservation.getStatus())
                        .build()
        ).toList();
    }
}
