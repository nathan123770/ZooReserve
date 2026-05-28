export type RoleCode = 'VISITOR' | 'ADMIN' | 'CHECKER';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface LoginUser {
  id: number;
  username: string;
  displayName: string;
  role: RoleCode;
}

export interface LoginResponse {
  token: string;
  user: LoginUser;
}

export interface TicketType {
  code: string;
  name: string;
  price: number;
  description: string;
  annualPass: boolean;
}

export interface TicketInventory {
  date: string;
  session: string;
  ticketTypeCode: string;
  capacity: number;
  remaining: number;
}

export interface OrderRecord {
  id: number;
  orderNo: string;
  visitDate: string;
  session: string;
  peopleCount: number;
  amount: number;
  orderStatus: string;
  paymentStatus: string;
  createdAt: string;
}

export interface ActivityRecord {
  id: number;
  title: string;
  category: string;
  startTime: string;
  capacity: number;
  signedCount: number;
  location: string;
}
