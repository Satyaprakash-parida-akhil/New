import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { PendingRegistrationsComponent } from './features/admin/registrations/pending-registrations.component';
import { ApprovedUsersComponent } from './features/admin/registrations/approved-users.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { HomeComponent } from './features/home/home.component';
import { ProfileComponent } from './features/profile/profile.component';
import { PaymentComponent } from './features/payment/payment.component';
import { AdminPaymentsComponent } from './features/admin/payments/admin-payments.component';
import { WelcomeComponent } from './features/welcome/welcome.component';
import { FacilitiesComponent } from './features/facilities/facilities.component';
import { RoleGuard } from './core/guards/role.guard';
import { ActiveGuard } from './core/guards/active.guard';

export const routes: Routes = [
    { path: 'welcome', component: WelcomeComponent },
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    { path: 'forgot-password', component: ForgotPasswordComponent },
    { path: 'reset-password', component: ResetPasswordComponent },
    { path: 'home', component: HomeComponent, canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.HOME' } },
    { path: 'profile', component: ProfileComponent, canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.PROFILE' } },
    { path: 'payment', component: PaymentComponent, canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.PAYMENT' } },
    {
        path: 'facilities', component: FacilitiesComponent,
        canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.FACILITIES' }
    },
    { path: 'description', loadComponent: () => import('./features/description/description.component').then(m => m.DescriptionComponent), canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.GROWTH_PLAN' } },
    { path: '', redirectTo: '/welcome', pathMatch: 'full' },
    {
        path: 'dashboard', component: DashboardComponent,
        canActivate: [RoleGuard],
        data: { breadcrumb: 'NAV.DASHBOARD', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/registrations', component: PendingRegistrationsComponent,
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.USER_REQUESTS', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/users', component: ApprovedUsersComponent,
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.APPROVED_USERS', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/dealer-management',
        loadComponent: () => import('./features/admin/dealer-management/dealer-management.component').then(m => m.DealerManagementComponent),
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.DEALER_MANAGEMENT', roles: ['ROLE_ADMIN', 'ROLE_DEALER', 'Dealer'] }
    },
    {
        path: 'admin/dealer-area-assignment',
        loadComponent: () => import('./features/admin/dealer-area-assignment/dealer-area-assignment.component').then(m => m.DealerAreaAssignmentComponent),
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.DEALER_AREA_ASSIGNMENT', roles: ['ROLE_ADMIN', 'ROLE_DEALER', 'Dealer'] }
    },
    {
        path: 'admin/payments', component: AdminPaymentsComponent,
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.PAYMENT_REVIEW', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/masters',
        loadComponent: () => import('./features/admin/masters/master-list.component').then(m => m.MasterListComponent),
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.MASTER_MGMT', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/masters/:headerId',
        loadComponent: () => import('./features/admin/masters/master-detail-page.component').then(m => m.MasterDetailPageComponent),
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.MASTER_DETAIL', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'admin/screen-assignment', loadComponent: () => import('./features/admin/screen-assignment/screen-assignment.component').then(m => m.ScreenAssignmentComponent),
        canActivate: [RoleGuard], data: { breadcrumb: 'NAV.SCREEN_ASSIGNMENT', roles: ['ROLE_ADMIN'] }
    },
    {
        path: 'referral-tree', loadComponent: () => import('./features/referral-tree/referral-tree.component').then(m => m.ReferralTreeComponent),
        canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.REFERRAL_TREE' }
    },
    {
        path: 'offer', loadComponent: () => import('./features/user/offer/offer.component').then(m => m.OfferComponent),
        canActivate: [ActiveGuard], data: { breadcrumb: 'NAV.OFFER' }
    },
    { path: '**', redirectTo: '', pathMatch: 'full' }
];
