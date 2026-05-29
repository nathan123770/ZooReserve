import { deleteData, getData, postData, putData } from './http';
import type {
  ActivityRecord,
  AnnualPassRecord,
  LoginResponse,
  MemberCouponRecord,
  MemberProfileRecord,
  OrderRecord,
  PaymentResponse,
  RoleCode,
  TicketInventory,
  TicketType,
} from '@/types/api';

export const authApi = {
  login: (username: string, password: string, role: RoleCode) =>
    postData<LoginResponse>('/auth/login', { username, password, role }),
  register: (payload: { username: string; password: string; phone: string; displayName: string }) =>
    postData<LoginResponse>('/auth/register', payload),
};

export const ticketApi = {
  types: () => getData<TicketType[]>('/tickets/types'),
  inventory: (date: string, session: string) => getData<TicketInventory[]>('/tickets/inventory', { date, session }),
};

export const orderApi = {
  create: (payload: unknown) => postData<OrderRecord>('/orders', payload),
  my: () => getData<OrderRecord[]>('/orders/my'),
  qrcode: (id: number) => getData<{ orderNo: string; qrContent: string; visitDate: string; entranceNotice: string }>(`/orders/${id}/qrcode`),
  cancel: (id: number) => postData<OrderRecord>(`/orders/${id}/cancel`),
  refund: (id: number | string) => postData<OrderRecord>(`/orders/${id}/refund`),
};

export const paymentApi = {
  prepay: (orderNo: string) =>
    postData<PaymentResponse>('/payments/prepay', { orderNo, channel: 'MOCK' }),
};

export const memberApi = {
  profiles: () => getData<MemberProfileRecord[]>('/member/profiles'),
  coupons: () => getData<MemberCouponRecord[]>('/member/coupons'),
  annualPasses: () => getData<AnnualPassRecord[]>('/member/annual-passes'),
  createProfile: (payload: unknown) => postData<MemberProfileRecord>('/member/profiles', payload),
  updateProfile: (id: number, payload: unknown) => putData<MemberProfileRecord>(`/member/profiles/${id}`, payload),
  deleteProfile: (id: number) => deleteData<Record<string, unknown>>(`/member/profiles/${id}`),
  claimCoupon: (couponId: number) => postData<Record<string, unknown>>(`/member/coupons/${couponId}/claim`),
  purchaseAnnualPass: (payload: unknown) => postData<Record<string, unknown>>('/member/annual-passes/purchase', payload),
  renewAnnualPass: (id: number) => postData<Record<string, unknown>>(`/member/annual-passes/${id}/renew`),
  addAnnualPassHolder: (id: number, payload: unknown) => postData<Record<string, unknown>>(`/member/annual-passes/${id}/holders`, payload),
};

export const activityApi = {
  list: () => getData<ActivityRecord[]>('/activities'),
  signup: (id: number) => postData<{ status: string }>(`/activities/${id}/signup`),
};

export const adminApi = {
  dashboard: () => getData<Record<string, unknown>>('/admin/dashboard/summary'),
  records: (domain: string, params?: Record<string, unknown>) => getData<{ records: unknown[] }>(domain === 'orders' ? '/admin/orders' : `/admin/${domain}`, params),
  create: (domain: string, payload: Record<string, unknown>) => postData<Record<string, unknown>>(`/admin/${domain}`, payload),
  update: (domain: string, id: number, payload: Record<string, unknown>) =>
    putData<Record<string, unknown>>(`/admin/${domain}/${id}`, payload),
  updateInventory: (payload: Record<string, unknown>) => putData<Record<string, unknown>>('/admin/tickets/inventory', payload),
  toggleStatus: (domain: string, id: number, status: string) =>
    putData<Record<string, unknown>>(`/admin/${domain}/${id}/status`, { status }),
  toggleStatusWithPayload: (domain: string, id: number, payload: Record<string, unknown>) =>
    putData<Record<string, unknown>>(`/admin/${domain}/${id}/status`, payload),
  approveRefund: (refundId: number) => postData<Record<string, unknown>>(`/admin/refunds/${refundId}/approve`),
  manualCheckin: (payload: Record<string, unknown>) => postData<Record<string, unknown>>('/admin/checkins/manual', payload),
};

export const checkinApi = {
  scan: (qrContent: string) => postData<Record<string, unknown>>('/checkin/scan', { qrContent, checkerId: 1 }),
  search: (keyword: string) => getData<Record<string, unknown>>('/checkin/order/search', { orderNo: keyword, phone: keyword }),
  manual: (payload: unknown) => postData<Record<string, unknown>>('/checkin/manual', payload),
};
