import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  authService = inject(AuthService);
  http = inject(HttpClient);

  // Authentication state
  username = '';
  password = '';

  // Data state
  events = signal<any>(null);
  eventId = '';
  requestedTickets = 1;

  // Debug Console state
  debugLogs = signal<string[]>([]);

  log(msg: string, isError = false) {
    const timestamp = new Date().toISOString();
    const prefix = isError ? '[ERROR]' : '[LOG]';
    this.debugLogs.update(logs => [...logs, `${prefix} [${timestamp}] ${msg}`]);
  }

  handleLogin() {
    this.log(`Wysłano POST /api/auth/login dla użytkownika: ${this.username}`);
    this.authService.login(this.username, this.password).subscribe({
      next: (res) => this.log(`Zalogowano pomyślnie. Token otrzymany.`),
      error: (err: HttpErrorResponse) => this.log(`HTTP ${err.status} - ${err.message}`, true)
    });
  }

  fetchEvents() {
    this.log(`Wysłano GET /api/events...`);
    this.http.get('http://localhost:8080/api/events').subscribe({
      next: (data) => {
        this.log(`Pobrano wydarzenia. Status: 200 OK`);
        this.events.set(data);
      },
      error: (err: HttpErrorResponse) => this.log(`HTTP ${err.status} - ${err.message}`, true)
    });
  }

  submitReservation() {
    const payload = {
      eventId: Number(this.eventId),
      userId: 1, // hardcoded for test or we could extract from JWT
      requestedTickets: this.requestedTickets
    };
    this.log(`Wysłano POST /api/reservations z payloadem: ${JSON.stringify(payload)}`);
    
    this.http.post('http://localhost:8080/api/reservations', payload).subscribe({
      next: (res) => this.log(`Rezerwacja przyjęta. Status: 200/201. Odpowiedź: ${JSON.stringify(res)}`),
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || err.message || JSON.stringify(err.error);
        this.log(`HTTP ${err.status} - ${errorMsg}`, true);
      }
    });
  }
}
