import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';
  
  public token = signal<string | null>(localStorage.getItem('jwt_token'));

  constructor(private http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<{ token: string }>(`${this.baseUrl}/login`, { username, password })
      .pipe(
        tap(response => {
          if (response.token) {
            localStorage.setItem('jwt_token', response.token);
            this.token.set(response.token);
          }
        })
      );
  }

  logout() {
    localStorage.removeItem('jwt_token');
    this.token.set(null);
  }

  getToken(): string | null {
    return this.token();
  }
}
