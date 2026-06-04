import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const toastService = inject(ToastService);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage = 'An unexpected error occurred';

            if (error.error instanceof ErrorEvent) {
                // Client-side error
                errorMessage = error.error.message;
                console.error('Client-side error:', error.error.message);
            } else {
                // Server-side error
                // The backend returns a response in the form of { success: false, message: "..." }
                // We prefer the clear message from the backend.
                errorMessage = error.error?.message || error.message || `Error Code: ${error.status}`;
                console.error(`Server-side error [${error.status}]:`, error.error);
            }

            // Show toast if it's not a 401 (handled by auth logic) or 403 (handled by auth logic)
            if (error.status !== 401 && error.status !== 403) {
                // Only show if we have a meaningful message
                toastService.showError(errorMessage, 'Error');
            }

            return throwError(() => error);
        })
    );
};
