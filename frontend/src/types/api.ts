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
  dailyCapacity: number;
  dailyRemaining: number;
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
  orderType: string;
  originalAmount: number;
  discountAmount: number;
  items: OrderItemRecord[];
}

export interface OrderItemRecord {
  ticketTypeCode: string;
  ticketTypeName: string;
  quantity: number;
  unitPrice: number;
}

export interface PaymentResponse {
  orderNo: string;
  channel: string;
  amount: number;
  paymentStatus: string;
  mockPayUrl: string;
}

export interface MemberProfileRecord {
  id: number;
  name?: string;
  realName?: string;
  idCard?: string;
  idcard?: string;
  idCardNo?: string;
  phone: string;
  relation?: string;
  isDefault?: boolean | number;
  isdefault?: boolean | number;
}

export interface MemberCouponRecord {
  id: number;
  name: string;
  threshold?: string;
  thresholdAmount?: number;
  discountValue?: number;
  discountType?: string;
  status: string;
  expiresAt?: string;
}

export interface AnnualPassRecord {
  id: number;
  name: string;
  status: string;
  expiresAt: string;
  boundVisitors?: string[] | string;
  benefits?: string[] | string;
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
