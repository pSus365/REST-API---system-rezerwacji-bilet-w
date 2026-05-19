package com.example.demo.config;

import com.example.demo.model.AppUser;
import com.example.demo.model.Event;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Tworzenie testowego użytkownika
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser testUser = AppUser.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin"))
                    .role("USER")
                    .build();
            userRepository.save(testUser);
        }

        // Tworzenie testowego wydarzenia
        if (eventRepository.count() == 0) {
            Event testEvent = Event.builder()
                    .name("Wielki Koncert Finałowy")
                    .description("Niesamowite wydarzenie muzyczne")
                    .eventDate(LocalDateTime.now().plusDays(10))
                    .totalTickets(100)
                    .availableTickets(100)
                    .build();
            
            Event testEvent2 = Event.builder()
                    .name("Konferencja IT - Tech 2026")
                    .description("Największa konferencja o nowościach ze świata tech")
                    .eventDate(LocalDateTime.now().plusDays(5))
                    .totalTickets(50)
                    .availableTickets(50)
                    .build();

            eventRepository.save(testEvent);
            eventRepository.save(testEvent2);
        }
    }
}
