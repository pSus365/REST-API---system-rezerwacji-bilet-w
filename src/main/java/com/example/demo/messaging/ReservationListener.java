package com.example.demo.messaging;

import com.example.demo.model.ReservationStatus;
import com.example.demo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationListener {

    private final ReservationRepository reservationRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleReservationMessage(Long reservationId) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
            log.info("Symulacja: Wysłano maila z biletem dla rezerwacji nr: {}", reservationId);
        });
    }
}
