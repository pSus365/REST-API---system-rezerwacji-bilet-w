package com.example.demo.service;

import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.dto.ReservationResponseDTO;
import com.example.demo.exception.NotEnoughTicketsException;
import com.example.demo.messaging.RabbitMQConfig;
import com.example.demo.model.AppUser;
import com.example.demo.model.Event;
import com.example.demo.model.Reservation;
import com.example.demo.model.ReservationStatus;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ReservationService reservationService;

    private AppUser mockUser;
    private Event mockEvent;
    private ReservationRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        mockUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .build();

        mockEvent = Event.builder()
                .id(1L)
                .name("Test Event")
                .availableTickets(10)
                .build();

        validRequest = new ReservationRequestDTO(1L, 1L, 2);
    }

    @Test
    void createReservation_whenDataIsCorrect_savesReservationAndSendsMessage() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Reservation mockSavedReservation = Reservation.builder()
                .id(100L)
                .event(mockEvent)
                .user(mockUser)
                .status(ReservationStatus.PENDING)
                .reservationDate(LocalDateTime.now())
                .build();
                
        when(reservationRepository.save(any(Reservation.class))).thenReturn(mockSavedReservation);

        // Act
        ReservationResponseDTO response = reservationService.createReservation(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Test Event", response.eventName());
        assertEquals(8, mockEvent.getAvailableTickets()); // 10 - 2 = 8

        verify(eventRepository, times(1)).save(mockEvent);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME), 
                eq(RabbitMQConfig.ROUTING_KEY), 
                eq(100L)
        );
    }

    @Test
    void createReservation_whenNotEnoughTickets_throwsExceptionAndDoesNotSendMessage() {
        // Arrange
        mockEvent.setAvailableTickets(1); // Only 1 available, but requested 2
        when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThrows(NotEnoughTicketsException.class, () -> reservationService.createReservation(validRequest));

        verify(eventRepository, never()).save(any(Event.class));
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void createReservation_whenEventNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> reservationService.createReservation(validRequest));

        verify(appUserRepository, never()).findById(anyLong());
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
