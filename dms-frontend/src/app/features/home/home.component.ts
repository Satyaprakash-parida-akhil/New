import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, RouterModule, TranslateModule],
    templateUrl: './home.component.html',
    styleUrl: './home.component.css'
})
export class HomeComponent { }
