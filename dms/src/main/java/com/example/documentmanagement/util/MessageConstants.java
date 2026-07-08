package com.example.documentmanagement.util;

import com.example.documentmanagement.exception.UtilityClassInstantiationException;

public final class MessageConstants {

    private MessageConstants() {
        throw new UtilityClassInstantiationException();
    }

    public static final class Success {
        private Success() {}
        
        // General Auth
        public static final String OTP_VERIFIED = "OTP verified successfully";
        public static final String OTP_VERIFIED_RESET = "OTP verified. You can now reset your password.";
        public static final String REGISTRATION_SUCCESSFUL = "Registration successful. You can now log in.";
        public static final String PASSWORD_RESET_SUCCESSFUL = "Password reset successful";
        public static final String LOGIN_SUCCESS = "Login successful";
        
        // Profile
        public static final String PROFILE_LOADED = "Profile loaded";
        public static final String PROFILE_UPDATED = "Profile updated";
        public static final String REFERRAL_CODE_GENERATED = "Referral code generated successfully";
        
        // Payments
        public static final String PAYMENTS_FETCHED = "Payments fetched";
        public static final String PAYMENT_APPROVED = "Payment approved. Member is now active.";
        public static final String PAYMENT_REJECTED = "Payment rejected.";
        public static final String PAYMENT_ORDER_CREATED = "Payment order created successfully";
        public static final String PAYMENT_SUBMITTED = "Payment submitted successfully. Awaiting admin approval.";
        public static final String PAYMENT_RECORD_FETCHED = "Payment record fetched";
        public static final String PAYMENT_COMPLETED_ACTIVE = "Payment completed. Profile is now Active.";
        
        // Registrations
        public static final String REGISTRATIONS_PENDING = "Pending registrations fetched";
        public static final String REGISTRATIONS_INACTIVE = "Inactive registrations fetched";
        public static final String REGISTRATIONS_APPROVED = "Approved users fetched";
        public static final String USER_APPROVED = "User approved successfully";
        public static final String USER_REJECTED = "User rejected successfully";
        public static final String REQUEST_ARCHIVED = "Request archived";
        public static final String REQUEST_RESTORED = "Request restored to pending";
        
        // Master Data
        public static final String DROPDOWN_VALUES_RETRIEVED = "Dropdown values retrieved";
        public static final String MASTER_HEADERS_RETRIEVED = "Master headers retrieved";
        public static final String MASTER_HEADER_CREATED = "Master header created";
        public static final String MASTER_HEADER_UPDATED = "Master header updated";
        public static final String MASTER_HEADER_DEACTIVATED = "Master header deactivated";
        public static final String MASTER_HEADER_RESTORED = "Master header restored";
        public static final String MASTER_DETAILS_RETRIEVED = "Master details retrieved";
        public static final String MASTER_DETAIL_CREATED = "Master detail created";
        public static final String MASTER_DETAIL_UPDATED = "Master detail updated";
        public static final String MASTER_DETAIL_DEACTIVATED = "Master detail deactivated";
        public static final String MASTER_DETAIL_RESTORED = "Master detail restored";
        public static final String MASTER_HEADER_DELETED = "Master header permanently deleted";
        public static final String MASTER_DETAIL_DELETED = "Master detail permanently deleted";
        
        // Screen & Role Permissions
        public static final String ROLE_PERMISSIONS_RETRIEVED = "Role permissions retrieved";
        public static final String SCREEN_PERMISSIONS_UPDATED = "Screen permissions updated for role: ";
        public static final String USER_SCREEN_PERMISSIONS_RETRIEVED = "User screen permissions retrieved";
        public static final String USER_ROLES_UPDATED = "User roles updated successfully";
        
        // Subscriptions
        public static final String SUBSCRIPTION_PLANS_RETRIEVED = "Subscription plans retrieved successfully";
        
        // Users & Reviewers
        public static final String REVIEWERS_RETRIEVED = "Reviewers retrieved successfully";
        public static final String USERS_RETRIEVED = "Users retrieved successfully";
        public static final String USER_DETAILS_RETRIEVED = "User details retrieved";
        public static final String USER_CREATED = "User created successfully";
        public static final String USER_UPDATED = "User updated successfully";
        public static final String USER_SOFT_DELETED = "User soft deleted successfully";
        public static final String USER_BLOCKED = "User blocked successfully";
        public static final String USER_RESTORED = "User restored successfully";
        public static final String USER_PERMANENTLY_DELETED = "User permanently deleted from system";
        public static final String DASHBOARD_METRICS_RETRIEVED = "Dashboard metrics retrieved";
        public static final String REFERRAL_TREE_RETRIEVED = "Referral tree retrieved";
        
        // Dealer Module
        public static final String DEALER_VERIFICATION_SUBMITTED = "Dealer verification submitted successfully";
        public static final String DEALER_VERIFICATION_UPDATED = "Dealer verification updated";
        public static final String DEALER_AREA_UPDATED = "Dealer area updated successfully";
        public static final String DEALERS_FETCHED = "Dealers fetched";
        public static final String DEALER_PRODUCT_CREATED = "Product created";
        public static final String DEALER_PRODUCT_UPDATED = "Product updated";
        public static final String DEALER_PRODUCT_DELETED = "Product deleted";
        public static final String DEALER_PRODUCTS_FETCHED = "Products fetched";
        public static final String DEALER_AREA_ASSIGNED = "Area assigned";
        public static final String DEALER_ASSIGNMENT_UPDATED = "Assignment updated";
        public static final String DEALER_ASSIGNMENT_DELETED = "Assignment deleted";
        public static final String DEALER_ASSIGNMENTS_FETCHED = "Assignments fetched";
        public static final String DEALER_DISTRIBUTION_SUBMITTED = "Verification submitted";
        public static final String DEALER_DISTRIBUTION_UPDATED = "Distribution verification updated";
        public static final String DEALER_DISTRIBUTIONS_FETCHED = "Verifications fetched";
        // Files
        public static final String FILE_UPLOADED = "File uploaded successfully";
        public static final String FILE_DELETED = "File deleted successfully";
        public static final String FILE_PERMANENTLY_DELETED = "File permanently deleted";
        public static final String FILE_RECOVERED = "File recovered successfully";
        public static final String FILE_REPLACED = "File replaced successfully";
        public static final String EXCEL_EXPORT_SUCCESS = "Excel exported successfully";

        // Location & Search Success
        public static final String STATES_FETCHED = "States fetched successfully";
        public static final String DISTRICTS_FETCHED = "Districts fetched successfully";
        public static final String BLOCKS_FETCHED = "Blocks fetched successfully";
        public static final String PIN_CODES_FETCHED = "PIN Codes fetched successfully";
        public static final String FILTER_OPTIONS_RETRIEVED = "Filter options retrieved successfully";
        public static final String SEARCH_COMPLETED = "Search completed";
        public static final String FILE_RETRIEVED = "File retrieved successfully";
    }

    public static final class Error {
        private Error() {}
        
        // Auth & Access
        public static final String UNAUTHORIZED_ACCESS = "msg.unauthorized.access";
        public static final String ACCESS_DENIED_DOWNLINE = "Access Denied: User is not in your downline";
        public static final String ACCESS_DENIED_OWN_DETAILS = "Access Denied: You can only edit your own details";
        public static final String USER_NOT_FOUND = "User not found";
        public static final String ROLE_NOT_FOUND = "Role not found";
        public static final String INVALID_REFERRAL_CODE = "Invalid referral/promo code";
        
        // OTP & Communication
        public static final String SMS_FAILED = "Failed to send OTP via SMS. Please try again.";
        public static final String EMAIL_OTP_FAILED = "Could not send OTP email. Please try again later.";
        public static final String EMAIL_NOTIFICATION_FAILED = "Account approved, but notification email failed to send.";
        public static final String OTP_NOT_FOUND = "Valid OTP not found for this identifier";
        public static final String OTP_EXPIRED = "OTP has expired";
        public static final String OTP_INVALID = "Invalid OTP code";
        
        // Payments
        public static final String PAYMENT_ALREADY_PENDING = "You already have a pending payment under review. Please wait for admin approval.";
        public static final String PAYMENT_INVALID_SIMULATED = "Invalid simulated payment transaction.";
        public static final String PAYMENT_SIGNATURE_FAILED = "Razorpay signature verification failed. Payment was rejected.";
        public static final String PAYMENT_VERIFICATION_REQUIRED = "Transaction verification details are required.";
        public static final String PAYMENT_NOT_PENDING_APPROVAL = "Only PENDING payments can be approved. Current status: ";
        public static final String PAYMENT_NOT_PENDING_REJECTION = "Only PENDING payments can be rejected. Current status: ";
        public static final String PAYMENT_NOT_FOUND = "Payment not found";
        public static final String SUBSCRIPTION_PLAN_NOT_FOUND = "Subscription plan not found";
        
        // Files
        public static final String FILE_NOT_FOUND = "File not found";
        public static final String FILE_TOO_LARGE = "File size exceeds the allowed limit";
        public static final String UNSUPPORTED_FORMAT = "Unsupported file format";
        public static final String DUPLICATE_FILE = "A file with the same name and size already exists";
        public static final String UPLOAD_FAILED = "Failed to upload file";
        public static final String FILE_STORAGE_ERROR = "An error occurred while storing the file";
        public static final String INVALID_FILE = "Invalid file or file is empty";
        public static final String EXCEL_EXPORT_FAILED = "Failed to export Excel file";
        
        // System
        public static final String UNEXPECTED_ERROR = "msg.error.unexpected";
        public static final String INTERNAL_SERVER_ERROR = "An internal server error occurred. Please try again later.";
        public static final String ERROR_BAD_CREDENTIALS = "msg.error.bad.credentials";
        
        // Dealer Module
        public static final String DEALER_NOT_FOUND = "Dealer not found";
        public static final String DEALER_PRODUCT_NOT_FOUND = "Product not found";
        public static final String DEALER_ASSIGNMENT_NOT_FOUND = "Assignment not found";
        public static final String DEALER_DISTRIBUTION_NOT_FOUND = "Distribution verification not found";
    }

    public static final class Validation {
        private Validation() {}
        
        public static final String USERNAME_TAKEN = "Username is already taken";
        public static final String EMAIL_REGISTERED = "Email is already registered";
        public static final String EMAIL_IN_USE = "The provided email is already in use by another account";
        public static final String PHONE_REGISTERED = "Phone number is already registered";
        public static final String PHONE_IN_USE = "The provided phone number is already registered to another account";
        
        public static final String REFERRAL_CODE_EXISTS = "Referral code already generated";
        public static final String EMAIL_REQUIRED = "Email address is required";
        public static final String PHONE_REQUIRED = "Phone number is required";
        public static final String VALIDATION_ERROR = "msg.validation.error";
    }
}
