import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { PendingRegistrationsComponent } from './features/admin/registrations/pending-registrations.component';
import { ApprovedUsersComponent } from './features/admin/registrations/approved-users.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { UploadComponent } from './features/upload/upload.component';
import { ReviewComponent } from './features/review/review.component';
import { AssignmentComponent } from './features/assignment/assignment.component';
import { HomeComponent } from './features/home/home.component';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    { path: 'forgot-password', component: ForgotPasswordComponent },
    { path: 'reset-password', component: ResetPasswordComponent },
    { path: 'home', component: HomeComponent, data: { breadcrumb: 'NAV.HOME' } },
    { path: 'admin/registrations', component: PendingRegistrationsComponent, data: { breadcrumb: 'NAV.USER_REQUESTS' } },
    { path: 'admin/users', component: ApprovedUsersComponent, data: { breadcrumb: 'NAV.APPROVED_USERS' } },
    { path: 'dashboard', component: DashboardComponent, data: { breadcrumb: 'NAV.DASHBOARD' } },
    { path: 'docs/upload', component: UploadComponent, data: { breadcrumb: 'NAV.UPLOAD' } },
    { path: 'reviews/pending', component: ReviewComponent, data: { breadcrumb: 'NAV.REVIEW_QUEUE' } },
    { path: 'admin/assign', component: AssignmentComponent, data: { breadcrumb: 'NAV.ASSIGN_DOC' } },
    { path: '', redirectTo: '/home', pathMatch: 'full' }
];
