# Kompleksowy System Rezerwacji Biletów (REST API + Angular)

## 📌 Odnośniki do projektu
* **Wideo demonstracyjne (YouTube):** `[TUTAJ_WKLEJ_LINK_DO_YOUTUBE]`
* **Repozytorium GitHub:** [https://github.com/pSus365/REST-API---system-rezerwacji-bilet-w.git](https://github.com/pSus365/REST-API---system-rezerwacji-bilet-w.git)

---

## 🏗 Wstęp i Architektura Systemu

Zaprojektowałem i zaimplementowałem ten projekt w modelu architektury rozproszonej opartej na wzorcu klient-serwer. Po stronie serwerowej stworzyłem wydajne, bezstanowe (Stateless) REST API, a część kliencką zrealizowałem w architekturze Single Page Application (SPA).

Aplikację backendową oparłem o architekturę warstwową (Layered Architecture), aby zapewnić separację odpowiedzialności (SoC - Separation of Concerns):
* **Warstwa Prezentacji (Controller):** Przechwytuje żądania HTTP od klienta i deleguje zadania głębiej.
* **Warstwa Logiki Biznesowej (Service):** W niej zawarłem główną "mądrość" systemu (np. matematyczne kalkulacje dostępności biletów i transakcyjność).
* **Warstwa Persystencji (Repository):** Wykorzystałem interfejsy `JpaRepository`, by komunikować się z bazą danych bez pisania czystego kodu SQL.

## 🛠 Stos Technologiczny

W moim systemie wykorzystałem następujące, nowoczesne technologie:
* **Backend:** Java 21, Spring Boot (Web, Data JPA, Security, AMQP), Jakarta Validation.
* **Baza danych:** H2 Database (relacyjna baza działająca in-memory, emulująca środowisko docelowe).
* **Komunikacja asynchroniczna:** RabbitMQ (uruchamiany jako kontener Docker).
* **Testowanie:** JUnit 5, Mockito.
* **Frontend:** Angular 17+ (architektura Standalone Components oraz Signals).

## 🔐 Bezpieczeństwo i Autoryzacja

Zrezygnowałem z konwencjonalnej autoryzacji bazującej na sesjach na rzecz tokenów JWT (JSON Web Tokens). System jest w pełni bezstanowy. Hasła użytkowników zapisuję do bazy po wcześniejszym zahaszowaniu silnym algorytmem kryptograficznym `BCryptPasswordEncoder`.

Napisałem własny filtr przechwytujący `JwtAuthenticationFilter`, który w locie analizuje nadchodzące żądania (z nagłówkiem `Authorization: Bearer <token>`). Jeśli sygnatura JWT jest prawidłowa, samodzielnie odtwarzam kontekst zabezpieczeń (`SecurityContextHolder`), dopuszczając użytkownika do chronionych zasobów (takich jak tworzenie rezerwacji czy ich pobieranie).

## 🔄 Komunikacja Asynchroniczna i Kolejki (RabbitMQ)

Aby system nie marnował zasobów i nie blokował głównego wątku HTTP podczas ciężkich operacji w tle (takich jak wysyłka wiadomości e-mail z wygenerowanym biletem PDF), wdrożyłem architekturę sterowaną zdarzeniami (Event-Driven Architecture) za pomocą RabbitMQ.

Po zatwierdzeniu nowej transakcji w bazie danych, w moim serwisie `ReservationService` wołam klasę `RabbitTemplate`, która publikuje wiadomość na kolejkę. Zaimplementowałem również klasę nasłuchującą `ReservationListener` z adnotacją `@RabbitListener`, która w sposób odseparowany, w tle konsumuje te wiadomości, symulując czasochłonną wysyłkę e-maila i ostatecznie zamykając status rezerwacji jako `CONFIRMED`.

## 🛡 Strategia Walidacji i Obsługi Błędów

Nie pozwoliłem, by do moich serwisów biznesowych wpadały przypadkowe, brudne dane od strony frontendu (tzw. podejście Fail-Fast). Odseparowałem modele bazy danych od tego, co przyjmuje kontroler, używając obiektów DTO (Data Transfer Objects). W DTO zaimplementowałem ostre zasady z pakietu `jakarta.validation` (np. `@Min(value = 1)` dla ilości biletów czy `@NotNull`).

Aby obsługa wyjątków nie robiła bałaganu w kontrolerach, zbudowałem klasę `GlobalExceptionHandler` korzystając ze wzorca `@RestControllerAdvice`. To jedno scentralizowane miejsce, które wyłapuje każdy wyjątek zrzucony w aplikacji (np. brak wystarczającej puli biletów, złe dane w formularzu) i automatycznie tłumaczy go na estetyczny obiekt JSON z precyzyjnym statusem HTTP (np. 400 Bad Request, 404 Not Found), zabezpieczając integralność odpowiedzi z serwera.

## 🗄 Model Bazy Danych i Logika (CRUD)

Zaimplementowałem warstwę persystencji z użyciem dostawcy Hibernate ORM. Wymodelowałem relacyjną bazę danych składającą się z trzech głównych encji:
1. `AppUser` - Przechowuje dane kont, role systemowe oraz zaszyfrowane hasła.
2. `Event` - Obiekt wydarzenia (m.in nazwa, dokładna data i całkowita / dostępna pula biletów).
3. `Reservation` - Tabela pośrednicząca zachowująca relację One-To-Many w stronę użytkowników oraz wydarzeń.

Każdorazowo przy wywołaniu POST na endpoint `/api/reservations` realizuję pełny zapis CRUD-owy. System wyciąga encję wydarzenia, sprawdza, czy wolnych biletów jest więcej niż żądana wartość, pomniejsza pule dostępności, zapisuje modyfikację w repozytorium, po czym inicjuje obiekt rezerwacji w statusie `PENDING`. Zapewnia to stabilność i odporność na wyprzedaż puli biletów powyżej dostępnego limitu.

## ⚙️ Testy Jednostkowe

Do weryfikacji bezbłędności logiki biznesowej, na której opiera się aplikacja (czyli m.in. walidacja liczby biletów), napisałem izolowane testy jednostkowe dla klasy `ReservationServiceTest`. Wykorzystałem framework JUnit 5 oraz bibliotekę Mockito do zamockowania działania repozytoriów Springa, co gwarantuje pełną, hermetyczną weryfikację logiki aplikacji bez obciążającego polegania na bazie danych.

---

## 🚀 Instrukcja Uruchomienia (Krok po Kroku)

Dzięki odpowiedniej konfiguracji projektu i skryptom deweloperskim, całe środowisko jest bardzo łatwe w instalacji.

### Krok 1: Inicjalizacja Message Brokera (RabbitMQ)
Moja aplikacja wymaga dostępu do kolejki RabbitMQ. Najszybciej uruchomisz ją przy wykorzystaniu narzędzia Docker:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
*(Broker wystartuje na porcie 5672, a konsolę zarządzania znajdziesz pod adresem http://localhost:15672).*

### Krok 2: Uruchomienie Serwera (Backend)
Z poziomu głównego katalogu (tam gdzie widoczny jest plik `pom.xml`) zbuduj i uruchom Spring Boota za pomocą Mavena:
```bash
mvn clean install
mvn spring-boot:run
```
*(Od tego momentu backend nasłuchuje portu 8080. Aplikacja dzięki mojej klasie `DataInitializer` automatycznie zaimplementuje w bazie w locie domyślnego użytkownika **admin** z hasłem **admin** oraz przykładowe wydarzenia).*

### Krok 3: Uruchomienie Klienckiej Aplikacji (Frontend)
Otwórz drugie, niezależne okno terminala i przejdź do folderu `frontend`. Uruchom polecenia NPM:
```bash
cd frontend
npm install
npm start
```
*(Interfejs graficzny uruchomi się w środowisku deweloperskim na porcie 4200).*

### Krok 4: Korzystanie z systemu
1. Wejdź na adres `http://localhost:4200`.
2. Zaloguj się wpisując: `admin` / `admin`. Otrzymasz token JWT.
3. Kliknij **[ POBIERZ WYDARZENIA ]**, wybierz wydarzenie z nowo wygenerowanej rozwijanej listy, ustal liczbę biletów i zrealizuj rezerwację!
4. Wykorzystaj przycisk **[ POBIERZ REZERWACJE ]**, aby sprawdzić ich faktyczny, zapisany stan w systemie.
5. (Opcjonalnie) Konsola in-memory H2 z podglądem mojej bazy danych działa pod URL: `http://localhost:8080/h2-console` (URL: `jdbc:h2:mem:ticketdb`, Username: `sa`, bez hasła).
