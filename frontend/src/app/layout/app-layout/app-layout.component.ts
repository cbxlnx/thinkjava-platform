import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { UsersService } from '../../core/users/users.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-layout.component.html',
  styleUrls: ['./app-layout.component.css'],
  
})


export class AppLayoutComponent {
  constructor(
    private router: Router,
    private users: UsersService,
    private auth: AuthService
  ) { }
  
  displayName = '';
  initials = 'U';

  
ngOnInit(): void {
  this.loadMe();
}

loadMe(): void {
  this.users.me().subscribe({
    next: (me) => {
      const first = (me.firstName ?? '').trim();
      this.displayName = first || 'User';
      this.initials = first ? first.charAt(0).toUpperCase() : 'U';
    },
    error: () => {
      this.displayName = 'User';
      this.initials = 'U';
    }
  });
}

logout(): void {
  this.auth.logout();
  this.router.navigate(['/login']);
}

  goQuiz() {
    this.router.navigate(['/diagnostic']);
  }
}
