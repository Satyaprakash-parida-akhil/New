export interface UserResponse {
    id: number;
    username: string;
    email: string;
    phoneNumber?: string;
    fullName?: string;
    gender?: string;
    dateOfBirth?: string;
    address?: string;
    block?: string;
    town?: string;
    state?: string;
    village?: string;
    landmark?: string;
    district?: string;
    country?: string;
    pinCode?: string;
    zone?: string;
    role?: string;
    referralCode?: string;
    referredByCode?: string;
    referredByName?: string;
    isActive?: boolean;
    isPaid?: boolean;
    createdAt?: string;
    photoUrl?: string;
}

export interface DashboardStats {
    totalUsers: number;
    activeUsers: number;
    inactiveUsers: number;
    paidUsers: number;
    unpaidUsers: number;
}

export interface ReferralNode {
    userId: number;
    username: string;
    fullName: string;
    email?: string;
    phoneNumber?: string;
    paymentStatus?: string;
    isActive: boolean;
    role?: string;
    level?: number;
    children: ReferralNode[];
    totalDownlineCount?: number;
    referralCount?: number;
    referralCode?: string;
    joinedDate?: string;
    createdAt?: string;
    referredByName?: string;
    referredByCode?: string;
}

export interface MasterHeader {
    id: number;
    dropdownName: string;  // maps to backend dropdownName field
    status: string;        // ACTIVE | INACTIVE
}

export interface MasterDetail {
    id: number;
    masterHeader?: { id: number; dropdownName: string };
    displayName: string;   // maps to backend displayName field
    parent?: { id: number; displayName: string } | null;
    status: string;        // ACTIVE | INACTIVE
}
