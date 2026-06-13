import {
  AfterViewInit,
  Component,
  ElementRef,
  NgZone,
  OnInit,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

// Google Identity Services injeta `google` no escopo global.
declare const google: any;

type Mode = 'login' | 'register';

@Component({
  selector: 'spartan-login',
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent implements OnInit, AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);

  private readonly googleBtn = viewChild<ElementRef<HTMLDivElement>>('googleBtn');

  protected readonly mode = signal<Mode>('login');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly googleEnabled = signal(false);

  // Campos do formulário (template-driven).
  protected name = '';
  protected email = '';
  protected password = '';

  ngOnInit(): void {
    this.auth.config().subscribe({
      next: (cfg) => {
        if (cfg.googleEnabled && cfg.googleClientId) {
          this.googleEnabled.set(true);
          this.loadGoogle(cfg.googleClientId);
        }
      },
      error: () => {
        /* config indisponível: segue só com email/senha */
      },
    });
  }

  ngAfterViewInit(): void {
    // O botão é renderizado após o script carregar (loadGoogle).
  }

  toggleMode(): void {
    this.mode.set(this.mode() === 'login' ? 'register' : 'login');
    this.error.set(null);
  }

  submit(): void {
    if (this.loading()) return;
    this.error.set(null);
    this.loading.set(true);

    const onError = (err: HttpErrorResponse) => {
      this.error.set(err.error?.message ?? 'Algo deu errado. Tente de novo.');
      this.loading.set(false);
    };
    const onSuccess = () => {
      this.loading.set(false);
      this.router.navigate(['/jogos']);
    };

    if (this.mode() === 'register') {
      this.auth
        .register({ name: this.name, email: this.email, password: this.password })
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.auth
        .login({ email: this.email, password: this.password })
        .subscribe({ next: onSuccess, error: onError });
    }
  }

  // ----- Google Identity Services -----
  private loadGoogle(clientId: string): void {
    const init = () => {
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (resp: { credential: string }) => this.onGoogle(resp.credential),
      });
      const el = this.googleBtn()?.nativeElement;
      if (el) {
        google.accounts.id.renderButton(el, {
          theme: 'filled_black',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          width: 320,
        });
      }
    };

    if (typeof google !== 'undefined' && google.accounts) {
      init();
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => init();
    document.head.appendChild(script);
  }

  private onGoogle(idToken: string): void {
    // O callback do Google roda fora da zona do Angular.
    this.zone.run(() => {
      this.loading.set(true);
      this.error.set(null);
      this.auth.loginWithGoogle(idToken).subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/jogos']);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(err.error?.message ?? 'Falha no login com Google.');
          this.loading.set(false);
        },
      });
    });
  }
}
