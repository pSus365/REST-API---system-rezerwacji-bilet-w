# System Rezerwacji Biletów (REST API)

Projekt zrealizowany w ramach zaliczenia na ocenę **5.0 (Bardzo dobry)**. Jest to kompletna aplikacja backendowa oparta na architekturze Spring Boot, oferująca pełen ekosystem rezerwacji biletów na wydarzenia.

## Spełnione Kryteria

### Ocena 3.0
- [x] **Działający Spring Boot**: Aplikacja uruchamia się bez błędów.
- [x] **Połączenie z bazą danych**: Skonfigurowana in-memory baza H2.
- [x] **CRUD dla encji**: Kompletne zarządzanie encją Rezerwacji (Reservation) oraz powiązanymi Użytkownikami i Wydarzeniami.
- [x] **Demo video**: Prezentacja podstawowego działania (link w sekcji wymagań formalnych).

### Ocena 4.0
- [x] **Poprawna struktura warstwowa**: Logiczny podział na warstwy `Controller`, `Service`, `Repository`, `Model`, `DTO`.
- [x] **DTO + Walidacja danych**: Obiekty transferowe dla requestów/response'ów oraz rygorystyczna walidacja przez `jakarta.validation` (`@Valid`, `@NotNull`, `@Min`, `@Email`).
- [x] **Obsługa błędów**: Globalny `@RestControllerAdvice` (`GlobalExceptionHandler`) gwarantujący czyste odpowiedzi JSON z odpowiednimi kodami HTTP (np. 400, 404).
- [x] **Security**: System logowania i rejestracji zabezpieczony algorytmem BCrypt oraz bezstanowa autoryzacja za pomocą tokenów **JWT**.

### Ocena 5.0
- [x] **Unit Testy**: Obejmujące warstwę serwisu napisane w **JUnit 5** z użyciem **Mockito**.
- [x] **Events LUB Kolejki**: Wdrożono **RabbitMQ**. Tworzenie rezerwacji wysyła wiadomość na kolejkę, co symuluje asynchroniczną wysyłkę biletów na e-mail i ostateczne potwierdzanie rezerwacji przez Listenery.
- [x] **Czysty kod**: Brak "zapachów" kodu, prawidłowe wstrzykiwanie zależności, separacja odpowiedzialności.
- [x] **Frontend**: Oczekiwana aplikacja w Angularze (implementowana jako osobny projekt kliencki).

## Technologie i Architektura

* **Java** (kompatybilność z nowymi wersjami)
* **Spring Boot** (Web, Data JPA, Security, AMQP, Validation)
* **Baza danych**: H2 Database
* **Kolejki komunikatów**: RabbitMQ
* **Zabezpieczenia**: Spring Security + JWT (`io.jsonwebtoken`) + BCryptPasswordEncoder
* **Testowanie**: JUnit 5, Mockito
* **Narzędzia pomocnicze**: Lombok, Maven

## Dokumentacja Uruchomienia

### Wymagania wstępne
1. Zainstalowane środowisko **Java** (JDK).
2. Zainstalowany **Maven**.
3. Działający serwer **RabbitMQ**. Najszybciej uruchomić go za pomocą dockera:
   ```bash
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```

### Kroki do uruchomienia
1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/pSus365/REST-API---system-rezerwacji-bilet-w.git
   cd REST-API---system-rezerwacji-bilet-w
   ```
2. Zbuduj aplikację:
   ```bash
   mvn clean install
   ```
3. Uruchom serwer Spring Boot:
   ```bash
   mvn spring-boot:run
   ```
4. Aplikacja nasłuchuje domyślnie na porcie `8080`.
5. *Opcjonalnie*: Konsola bazy danych H2 dostępna jest pod adresem: `http://localhost:8080/h2-console` (dostęp został wyłączony spod ochrony JWT w SecurityConfig).

## Endpointy API i Autoryzacja

**Endpointy otwarte (Publiczne):**
* `POST /api/auth/register` - Rejestracja nowego użytkownika. Wymaga podania username, e-maila i hasła.
* `POST /api/auth/login` - Logowanie. Zwraca wygenerowany token JWT.

**Endpointy chronione (Wymagają nagłówka `Authorization: Bearer <token>`):**
* `POST /api/reservations` - Tworzenie nowej rezerwacji. API weryfikuje token, sprawdza logikę biletów, aktualizuje bazę i asynchronicznie deleguje resztę do RabbitMQ.
* `GET /api/reservations/{id}` - Pobieranie potwierdzonej rezerwacji.

